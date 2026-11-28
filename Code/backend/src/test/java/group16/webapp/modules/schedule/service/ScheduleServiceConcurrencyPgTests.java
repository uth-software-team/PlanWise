package group16.webapp.modules.schedule.service;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.modules.assignment.api.AssignmentApi;
import group16.webapp.modules.availability.api.AvailabilityApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.api.ProposalStatus;
import group16.webapp.modules.schedule.api.SessionStatus;
import group16.webapp.modules.schedule.domain.ScheduleProposal;
import group16.webapp.modules.schedule.domain.ScheduleSession;
import group16.webapp.modules.schedule.dto.ConfirmRequest;
import group16.webapp.modules.schedule.dto.GenerateScheduleRequest;
import group16.webapp.modules.schedule.dto.ProposalBatchResponse;
import group16.webapp.modules.schedule.dto.SessionEditRequest;
import group16.webapp.modules.schedule.repository.ScheduleProposalRepository;
import group16.webapp.modules.schedule.repository.ScheduleSessionRepository;
import group16.webapp.modules.task.api.NewGroupTaskCommand;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.api.TaskSummary;
import group16.webapp.support.PostgresContainerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SA-17 on PostgreSQL 16: concurrent edit/confirm and generate/generate on one batch, through the real
 * ScheduleService and the V10 schema (row locks, {@code @Version}, partial unique index). The Task, Availability
 * and Assignment modules are stand-ins ({@code @Primary}, so they still win once the real modules exist); users
 * and tasks are real rows because of the V10 foreign keys. Not {@code @Transactional}: each service call commits.
 *
 * <p>Adds a nested {@code @TestConfiguration}, so this class gets its own Spring context (same container).
 */
class ScheduleServiceConcurrencyPgTests extends PostgresContainerTest {

    private static final int TIMEOUT_SECONDS = 30;

    /** 09:00 tomorrow in the default user timezone, so availability [09:00, 17:00) never crosses local midnight. */
    static final Instant BASE = java.time.LocalDate.now(ZoneId.of(DateUtils.DEFAULT_TIMEZONE)).plusDays(1)
            .atTime(9, 0).atZone(ZoneId.of(DateUtils.DEFAULT_TIMEZONE)).toInstant();
    /** Shared, mutable state of the stand-in TaskApi; reset before each test. */
    static final List<SchedulableTask> TASKS = new CopyOnWriteArrayList<>();

    private static final Set<String> REJECTED_409 = Set.of(ErrorCode.STALE_VERSION.name(), ErrorCode.DATA_CONFLICT.name());

    @TestConfiguration
    static class StandInModules {

        @Bean
        @Primary
        AvailabilityApi availabilityApi() {
            return new AvailabilityApi() {
                public boolean hasActiveRule(Long userId) { return true; }
                public List<TimeRange> availableIntervals(Long userId, Instant start, Instant end) {
                    return List.of(new TimeRange(BASE, BASE.plus(Duration.ofHours(8))));
                }
                public List<TimeRange> fixedEventOccurrences(Long userId, Instant start, Instant end) {
                    return List.of();
                }
            };
        }

        @Bean
        @Primary
        AssignmentApi assignmentApi() {
            return userId -> Set.of();
        }

        @Bean
        @Primary
        TaskApi taskApi() {
            return new TaskApi() {
                public List<SchedulableTask> schedulableTasks(Long userId, Collection<Long> acceptedGroupTaskIds) {
                    return List.copyOf(TASKS);
                }
                public Long createGroupTask(Long groupId, Long creatorUserId, NewGroupTaskCommand command) {
                    throw new UnsupportedOperationException();
                }
                public List<TaskSummary> summaries(Collection<Long> taskIds) {
                    return TASKS.stream()
                            .filter(t -> taskIds.contains(t.taskId()))
                            .map(t -> new TaskSummary(t.taskId(), null, "Task " + t.taskId(), t.deadline(),
                                    t.priority(), TaskStatus.TODO))
                            .toList();
                }
                public Optional<Long> findActiveGroupTaskGroupId(Long taskId) { return Optional.empty(); }
            };
        }
    }

    @Autowired private ScheduleService service;
    @Autowired private ScheduleProposalRepository proposalRepository;
    @Autowired private ScheduleSessionRepository sessionRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    private Long userId;

    @BeforeEach
    void setUp() {
        TASKS.clear();
        userId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'Pg') RETURNING id",
                Long.class, "pg-schedule-" + UUID.randomUUID() + "@example.com");
        Long taskId = jdbc.queryForObject(
                "INSERT INTO tasks (owner_user_id, title, description, priority, category, estimated_duration_minutes)"
                        + " VALUES (?, 'Pg task', '', 'HIGH', 'WORK', 60) RETURNING id",
                Long.class, userId);
        TASKS.add(new SchedulableTask(taskId, BASE.plus(Duration.ofDays(7)), TaskPriority.HIGH, 60,
                BASE.minus(Duration.ofDays(3))));
    }

    @AfterEach
    void cleanUp() {
        // Only this test's fixture rows, children first (V10 foreign keys).
        if (userId != null) {
            jdbc.update("DELETE FROM schedule_sessions WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM schedule_proposals WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM tasks WHERE owner_user_id = ?", userId);
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    private ProposalBatchResponse generate() {
        return service.generateProposal(userId, new GenerateScheduleRequest(BASE, BASE.plus(Duration.ofDays(1)), null));
    }

    private int storedVersion(Long proposalId) {
        return proposalRepository.findById(proposalId).orElseThrow().getVersion();
    }

    /** "OK", a bare 409 code, or details of anything else (for the failure message). */
    private static String outcome(Runnable action) {
        try {
            action.run();
            return "OK";
        } catch (AppException e) {
            return e.getErrorCode().name();
        } catch (OptimisticLockingFailureException e) {
            // Mapped to 409 STALE_VERSION by GlobalExceptionHandler.
            return ErrorCode.STALE_VERSION.name();
        } catch (RuntimeException e) {
            return "UNEXPECTED " + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("barrier not reached", e);
        }
    }

    /**
     * Transaction 1 confirms and holds the proposal row lock; meanwhile the same batch's session is edited with the
     * same version. The edit must wait for the lock, then exactly one of them wins, the other gets 409, and a
     * CONFIRMED session is never moved (BR-10). Contract test: it checks the outcome, not the lock order itself.
     */
    @Test
    void editDuringConfirmOfSameBatchIsRejectedAndConfirmedSessionStaysPut() throws Exception {
        ProposalBatchResponse proposal = generate();
        int v0 = storedVersion(proposal.proposalId());
        Long sessionId = proposal.sessions().get(0).sessionId();
        Instant movedStart = BASE.plus(Duration.ofHours(2));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        CountDownLatch confirmHoldsLock = new CountDownLatch(1);
        CountDownLatch releaseConfirm = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> confirm = executor.submit(() -> tx.execute(status -> {
                // Joins this transaction, so the proposal row lock is held until releaseConfirm.
                String result = outcome(() -> service.confirmProposal(userId, proposal.proposalId(), new ConfirmRequest(v0)));
                confirmHoldsLock.countDown();
                try {
                    if (!releaseConfirm.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("release signal not received");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return result;
            }));
            assertTrue(confirmHoldsLock.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "confirm did not finish its work");

            Future<String> edit = executor.submit(() -> outcome(() -> service.editSession(userId, sessionId,
                    new SessionEditRequest(movedStart, movedStart.plus(Duration.ofHours(1)), v0))));
            // Give the edit time to reach the locked rows before the confirm commits.
            assertThrows(TimeoutException.class, () -> edit.get(500, TimeUnit.MILLISECONDS),
                    "edit must not complete while the confirm transaction is open");

            releaseConfirm.countDown();
            List<String> outcomes = new ArrayList<>(List.of(
                    confirm.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), edit.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)));

            assertTrue(outcomes.remove("OK"), "exactly one of confirm/edit must succeed: " + outcomes);
            assertTrue(REJECTED_409.contains(outcomes.get(0)), "the other must be rejected with 409: " + outcomes);
        } finally {
            releaseConfirm.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        ScheduleSession stored = sessionRepository.findById(sessionId).orElseThrow();
        if (stored.getStatus() == SessionStatus.CONFIRMED) {
            assertEquals(BASE, stored.getStartAt(), "a CONFIRMED session must never be moved");
            assertEquals(ProposalStatus.CONFIRMED,
                    proposalRepository.findById(proposal.proposalId()).orElseThrow().getStatus());
        } else {
            assertEquals(SessionStatus.PROPOSED, stored.getStatus());
            assertEquals(movedStart, stored.getStartAt());
            assertEquals(ProposalStatus.PROPOSED,
                    proposalRepository.findById(proposal.proposalId()).orElseThrow().getStatus());
        }
        assertEquals(v0 + 1, storedVersion(proposal.proposalId()));
    }

    /**
     * Two generates have both read the same PROPOSED batch (same version) before either writes. Each transaction
     * loads that batch first, waits at the barrier, then calls generate in the same transaction, whose own lookup
     * returns the already-loaded instance. Both therefore cancel the batch against the same {@code @Version}: one
     * wins, the other waits for its row lock and then gets 409 STALE_VERSION, whatever the thread timing.
     * (With no batch yet, the partial unique index decides; see ScheduleMigrationPgTests.)
     */
    @Test
    void concurrentRegenerateLeavesExactlyOneProposedBatch() throws Exception {
        ProposalBatchResponse existing = generate();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        CyclicBarrier bothRead = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> regenerate = () -> outcome(() -> tx.executeWithoutResult(status -> {
                ScheduleProposal old = proposalRepository.findByUserIdAndStatus(userId, ProposalStatus.PROPOSED)
                        .orElseThrow();
                if (!old.getId().equals(existing.proposalId())) {
                    throw new IllegalStateException("read batch " + old.getId() + ", expected " + existing.proposalId());
                }
                await(bothRead);
                generate();
            }));
            Future<String> a = executor.submit(regenerate);
            Future<String> b = executor.submit(regenerate);
            List<String> outcomes = new ArrayList<>(Arrays.asList(
                    a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), b.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)));

            assertTrue(outcomes.remove("OK"), "one generate must succeed: " + outcomes);
            assertEquals(List.of(ErrorCode.STALE_VERSION.name()), outcomes,
                    "the other must be rejected with 409 STALE_VERSION: " + outcomes);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        long proposed = proposalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(p -> p.getStatus() == ProposalStatus.PROPOSED)
                .count();
        assertEquals(1, proposed, "exactly one PROPOSED batch must remain");
        assertEquals(ProposalStatus.CANCELLED,
                proposalRepository.findById(existing.proposalId()).orElseThrow().getStatus());
        assertTrue(sessionRepository.findByProposalIdOrderByStartAtAsc(existing.proposalId()).stream()
                .allMatch(s -> s.getStatus() == SessionStatus.CANCELLED));
    }
}
