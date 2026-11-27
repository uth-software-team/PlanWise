package group16.webapp.modules.schedule.service;

import group16.webapp.common.dto.ConflictErrorDto;
import group16.webapp.common.dto.ConflictErrorResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ConflictCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.assignment.api.AssignmentApi;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.availability.api.AvailabilityApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.api.ProposalStatus;
import group16.webapp.modules.schedule.api.SessionStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real ScheduleService + JPA (H2) with simple in-memory stand-ins for the Task/Availability/Assignment modules,
 * whose implementations are owned by other modules. Covers batch versioning, confirm rollback (BR-11),
 * CONFIRMED protection (BR-10) and concurrent confirm. Not {@code @Transactional}: each service call commits.
 */
@SpringBootTest
@ActiveProfiles("test")
class ScheduleServiceIntegrationTests {

    private static final AtomicLong USER_IDS = new AtomicLong(900_000);
    private static final long TASK_ID = 100L;
    private static final int TIMEOUT_SECONDS = 30;

    /** Shared, mutable state of the stand-in modules; reset before each test. */
    /** 09:00 tomorrow in the default user timezone, so availability [09:00, 17:00) never crosses local midnight. */
    static final Instant BASE = java.time.LocalDate.now(ZoneId.of(DateUtils.DEFAULT_TIMEZONE)).plusDays(1)
            .atTime(9, 0).atZone(ZoneId.of(DateUtils.DEFAULT_TIMEZONE)).toInstant();
    static final List<TimeRange> FIXED_EVENTS = new CopyOnWriteArrayList<>();
    static final List<SchedulableTask> TASKS = new CopyOnWriteArrayList<>();

    private static SchedulableTask task(long id, TaskPriority priority) {
        return new SchedulableTask(id, BASE.plus(Duration.ofDays(7)), priority, 60, BASE.minus(Duration.ofDays(3)));
    }

    @TestConfiguration
    static class StandInModules {

        @Bean
        AvailabilityApi availabilityApi() {
            return new AvailabilityApi() {
                public boolean hasActiveRule(Long userId) { return true; }
                public List<TimeRange> availableIntervals(Long userId, Instant start, Instant end) {
                    return List.of(new TimeRange(BASE, BASE.plus(Duration.ofHours(8))));
                }
                public List<TimeRange> fixedEventOccurrences(Long userId, Instant start, Instant end) {
                    return List.copyOf(FIXED_EVENTS);
                }
            };
        }

        @Bean
        AssignmentApi assignmentApi() {
            return userId -> Set.of();
        }

        @Bean
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
    @Autowired private UserRepository userRepository;

    private long userId;

    @BeforeEach
    void setUp() {
        FIXED_EVENTS.clear();
        TASKS.clear();
        TASKS.add(task(TASK_ID, TaskPriority.HIGH));
        userId = USER_IDS.incrementAndGet();
    }

    private void assertEditRejected(ProposalBatchResponse proposal, Long sessionId, Instant start, Instant end,
                                    ConflictCode expected) {
        int v0 = storedVersion(proposal.proposalId());
        ScheduleSession before = sessionRepository.findById(sessionId).orElseThrow();

        AppException ex = assertThrows(AppException.class,
                () -> service.editSession(userId, sessionId, new SessionEditRequest(start, end, v0)));

        assertEquals(ErrorCode.CONSTRAINT_VIOLATION, ex.getErrorCode());
        List<ConflictErrorDto> conflicts = ((ConflictErrorResponse) ex.getData()).conflicts();
        assertEquals(List.of(expected), conflicts.stream().map(ConflictErrorDto::code).toList());
        assertTrue(conflicts.stream().allMatch(c -> sessionId.equals(c.sessionId())));
        // Rolled back: same version, same times.
        assertEquals(v0, storedVersion(proposal.proposalId()));
        ScheduleSession after = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(before.getStartAt(), after.getStartAt());
        assertEquals(before.getEndAt(), after.getEndAt());
    }

    @Test
    void hc04_editCrossingLocalMidnightIsInvalidTimeRange() {
        ProposalBatchResponse proposal = generate();
        Long sessionId = proposal.sessions().get(0).sessionId();
        int v0 = storedVersion(proposal.proposalId());
        // Test users have no profile row, so the default timezone applies.
        ZoneId zone = ZoneId.of(DateUtils.DEFAULT_TIMEZONE);
        Instant localMidnight = BASE.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant();

        AppException ex = assertThrows(AppException.class, () -> service.editSession(userId, sessionId,
                new SessionEditRequest(localMidnight.minus(Duration.ofMinutes(30)),
                        localMidnight.plus(Duration.ofMinutes(30)), v0)));

        assertEquals(ErrorCode.INVALID_TIME_RANGE, ex.getErrorCode());
        assertEquals(v0, storedVersion(proposal.proposalId()));
        assertEquals(BASE, sessionRepository.findById(sessionId).orElseThrow().getStartAt());
    }

    /** A real user row, so the schedule module reads this timezone through UserApi. */
    private User realUser(String timezone) {
        User user = userRepository.save(new User("hc04-" + USER_IDS.incrementAndGet() + "@example.com",
                "not-a-real-hash", "HC04 User", timezone));
        userId = user.getId();
        return user;
    }

    private void changeTimezone(User user, String timezone) {
        User stored = userRepository.findById(user.getId()).orElseThrow();
        stored.updateProfile(stored.getFullName(), timezone);
        userRepository.save(stored);
    }

    @Test
    void hc04_confirmAfterTimezoneChangeCrossingMidnightIsConflictAndRolledBack() {
        User user = realUser("Asia/Ho_Chi_Minh");
        ProposalBatchResponse proposal = generate();
        Long sessionId = proposal.sessions().get(0).sessionId();
        // BASE = 09:00 +07 = 02:00Z. Move to 02:30Z–03:30Z: 09:30–10:30 in Ho Chi Minh City, valid.
        ProposalBatchResponse edited = service.editSession(userId, sessionId, new SessionEditRequest(
                BASE.plus(Duration.ofMinutes(30)), BASE.plus(Duration.ofMinutes(90)), storedVersion(proposal.proposalId())));
        // The same instants are 23:30–00:30 in Buenos Aires (UTC-3, no DST).
        changeTimezone(user, "America/Argentina/Buenos_Aires");

        AppException ex = assertThrows(AppException.class,
                () -> service.confirmProposal(userId, proposal.proposalId(), new ConfirmRequest(edited.version())));

        assertEquals(ErrorCode.CONSTRAINT_VIOLATION, ex.getErrorCode());
        List<ConflictErrorDto> conflicts = ((ConflictErrorResponse) ex.getData()).conflicts();
        assertEquals(List.of(ConflictCode.INVALID_TIME_RANGE), conflicts.stream().map(ConflictErrorDto::code).toList());
        assertEquals(sessionId, conflicts.get(0).sessionId());
        assertEquals(edited.version(), storedVersion(proposal.proposalId()));
        assertEquals(ProposalStatus.PROPOSED, proposalRepository.findById(proposal.proposalId()).orElseThrow().getStatus());
        assertEquals(SessionStatus.PROPOSED, sessionRepository.findById(sessionId).orElseThrow().getStatus());
    }

    @Test
    void hc04_confirmOfSessionEndingExactlyAtLocalMidnightSucceeds() {
        User user = realUser("Asia/Ho_Chi_Minh");
        ProposalBatchResponse proposal = generate();
        // Generated session 02:00Z–03:00Z is 23:00–24:00 in Buenos Aires: half-open, so still the same day.
        changeTimezone(user, "America/Argentina/Buenos_Aires");

        ProposalBatchResponse confirmed = service.confirmProposal(userId, proposal.proposalId(),
                new ConfirmRequest(storedVersion(proposal.proposalId())));

        assertEquals(ProposalStatus.CONFIRMED, confirmed.status());
    }

    @Test
    void editOntoFixedEventIsRejectedAndRolledBack() {
        FIXED_EVENTS.add(new TimeRange(BASE.plus(Duration.ofHours(3)), BASE.plus(Duration.ofHours(4))));
        ProposalBatchResponse proposal = generate();

        assertEditRejected(proposal, proposal.sessions().get(0).sessionId(),
                BASE.plus(Duration.ofMinutes(150)), BASE.plus(Duration.ofMinutes(210)), ConflictCode.FIXED_EVENT_OVERLAP);
    }

    @Test
    void editOutsideAvailabilityIsRejected() {
        ProposalBatchResponse proposal = generate();

        assertEditRejected(proposal, proposal.sessions().get(0).sessionId(),
                BASE.plus(Duration.ofMinutes(450)), BASE.plus(Duration.ofMinutes(510)), ConflictCode.OUTSIDE_AVAILABILITY);
    }

    @Test
    void editIntoThePastIsRejected() {
        ProposalBatchResponse proposal = generate();
        Instant past = Instant.now().minus(Duration.ofHours(2));

        AppException ex = assertThrows(AppException.class, () -> service.editSession(userId,
                proposal.sessions().get(0).sessionId(),
                new SessionEditRequest(past, past.plus(Duration.ofHours(1)), storedVersion(proposal.proposalId()))));

        assertEquals(ErrorCode.CONSTRAINT_VIOLATION, ex.getErrorCode());
        assertTrue(((ConflictErrorResponse) ex.getData()).conflicts().stream()
                .anyMatch(c -> c.code() == ConflictCode.PAST_TIME));
    }

    @Test
    void editLongerThanRemainingDurationIsRejected() {
        ProposalBatchResponse proposal = generate();

        assertEditRejected(proposal, proposal.sessions().get(0).sessionId(),
                BASE, BASE.plus(Duration.ofMinutes(90)), ConflictCode.DURATION_EXCEEDED);
    }

    @Test
    void editOntoSiblingSessionIsRejected() {
        TASKS.add(task(101L, TaskPriority.LOW));
        ProposalBatchResponse proposal = generate();
        var first = proposal.sessions().get(0);   // [BASE, BASE+60)
        var second = proposal.sessions().get(1);  // [BASE+60, BASE+120)

        assertEditRejected(proposal, second.sessionId(),
                first.startAt().plus(Duration.ofMinutes(30)), first.endAt().plus(Duration.ofMinutes(30)),
                ConflictCode.SESSION_OVERLAP);
    }

    @Test
    void editIsNotBlockedByAnotherSessionsConflict() {
        TASKS.add(task(101L, TaskPriority.LOW));
        ProposalBatchResponse proposal = generate();
        var first = proposal.sessions().get(0);
        var second = proposal.sessions().get(1);
        // A fixed event added later now overlaps the first session only.
        FIXED_EVENTS.add(new TimeRange(first.startAt(), first.endAt()));

        ProposalBatchResponse edited = service.editSession(userId, second.sessionId(), new SessionEditRequest(
                BASE.plus(Duration.ofHours(5)), BASE.plus(Duration.ofHours(6)), storedVersion(proposal.proposalId())));

        assertEquals(BASE.plus(Duration.ofHours(5)), edited.sessions().stream()
                .filter(s -> s.sessionId().equals(second.sessionId())).findFirst().orElseThrow().startAt());
    }

    private ProposalBatchResponse generate() {
        return service.generateProposal(userId,
                new GenerateScheduleRequest(BASE, BASE.plus(Duration.ofDays(1)), null));
    }

    private int storedVersion(Long proposalId) {
        return proposalRepository.findById(proposalId).orElseThrow().getVersion();
    }

    @Test
    void generatePlacesOneProposedSessionAtEarliestFreeTime() {
        ProposalBatchResponse proposal = generate();

        assertEquals(ProposalStatus.PROPOSED, proposal.status());
        assertEquals(1, proposal.sessions().size());
        var session = proposal.sessions().get(0);
        assertEquals(BASE, session.startAt());
        assertEquals(BASE.plus(Duration.ofMinutes(60)), session.endAt());
        assertEquals(SessionStatus.PROPOSED, session.status());
        assertTrue(proposal.unscheduledTasks().isEmpty());
    }

    @Test
    void eachMutationIncrementsBatchVersionExactlyOnce() {
        ProposalBatchResponse proposal = generate();
        int v0 = storedVersion(proposal.proposalId());
        Long sessionId = proposal.sessions().get(0).sessionId();

        service.editSession(userId, sessionId, new SessionEditRequest(
                BASE.plus(Duration.ofHours(1)), BASE.plus(Duration.ofHours(2)), v0));
        assertEquals(v0 + 1, storedVersion(proposal.proposalId()));

        ProposalBatchResponse confirmed = service.confirmProposal(userId, proposal.proposalId(), new ConfirmRequest(v0 + 1));
        assertEquals(v0 + 2, storedVersion(proposal.proposalId()));
        assertEquals(v0 + 2, confirmed.version());
        assertEquals(ProposalStatus.CONFIRMED, confirmed.status());
    }

    @Test
    void confirmWithStaleVersionIsRejected() {
        ProposalBatchResponse proposal = generate();
        int v0 = storedVersion(proposal.proposalId());

        AppException ex = assertThrows(AppException.class,
                () -> service.confirmProposal(userId, proposal.proposalId(), new ConfirmRequest(v0 + 5)));
        assertEquals(ErrorCode.STALE_VERSION, ex.getErrorCode());
        assertEquals(ProposalStatus.PROPOSED, proposalRepository.findById(proposal.proposalId()).orElseThrow().getStatus());
    }

    @Test
    void confirmWithConflictRollsBackWholeBatch() {
        ProposalBatchResponse proposal = generate();
        int v0 = storedVersion(proposal.proposalId());
        // A fixed event is added after generation and now overlaps the proposed session (HC-02).
        FIXED_EVENTS.add(new TimeRange(BASE.plus(Duration.ofMinutes(30)), BASE.plus(Duration.ofMinutes(90))));

        AppException ex = assertThrows(AppException.class,
                () -> service.confirmProposal(userId, proposal.proposalId(), new ConfirmRequest(v0)));

        assertEquals(ErrorCode.CONSTRAINT_VIOLATION, ex.getErrorCode());
        assertEquals(ProposalStatus.PROPOSED, proposalRepository.findById(proposal.proposalId()).orElseThrow().getStatus());
        assertEquals(v0, storedVersion(proposal.proposalId()));
        assertTrue(sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.proposalId()).stream()
                .allMatch(s -> s.getStatus() == SessionStatus.PROPOSED));
    }

    @Test
    void confirmedSessionCannotBeEditedOrDeleted() {
        ProposalBatchResponse proposal = generate();
        int v0 = storedVersion(proposal.proposalId());
        ProposalBatchResponse confirmed = service.confirmProposal(userId, proposal.proposalId(), new ConfirmRequest(v0));
        Long sessionId = confirmed.sessions().get(0).sessionId();

        AppException edit = assertThrows(AppException.class, () -> service.editSession(userId, sessionId,
                new SessionEditRequest(BASE.plus(Duration.ofHours(3)), BASE.plus(Duration.ofHours(4)), confirmed.version())));
        AppException delete = assertThrows(AppException.class,
                () -> service.deleteSession(userId, sessionId, confirmed.version()));

        assertEquals(409, edit.getErrorCode().getStatus().value());
        assertEquals(409, delete.getErrorCode().getStatus().value());
        ScheduleSession stored = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SessionStatus.CONFIRMED, stored.getStatus());
        assertEquals(BASE, stored.getStartAt());
    }

    @Test
    void regenerateKeepsConfirmedSessionsAndDoesNotOverlapThem() {
        ProposalBatchResponse first = generate();
        service.confirmProposal(userId, first.proposalId(), new ConfirmRequest(storedVersion(first.proposalId())));

        // The only task is fully covered by the confirmed session, so nothing new is proposed.
        ProposalBatchResponse second = generate();

        assertTrue(second.sessions().isEmpty());
        assertEquals(SessionStatus.CONFIRMED,
                sessionRepository.findById(first.sessions().get(0).sessionId()).orElseThrow().getStatus());
    }

    @Test
    void concurrentConfirmOfSameBatchSucceedsExactlyOnce() throws Exception {
        ProposalBatchResponse proposal = generate();
        int v0 = storedVersion(proposal.proposalId());

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> confirm = () -> {
                barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                try {
                    service.confirmProposal(userId, proposal.proposalId(), new ConfirmRequest(v0));
                    return "OK";
                } catch (AppException e) {
                    return e.getErrorCode().name();
                } catch (OptimisticLockingFailureException e) {
                    // Mapped to 409 STALE_VERSION by GlobalExceptionHandler.
                    return ErrorCode.STALE_VERSION.name();
                }
            };
            Future<String> a = executor.submit(confirm);
            Future<String> b = executor.submit(confirm);
            List<String> outcomes = new ArrayList<>(Arrays.asList(
                    a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), b.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)));

            assertTrue(outcomes.remove("OK"), "one confirm must succeed: " + outcomes);
            assertTrue(Set.of(ErrorCode.STALE_VERSION.name(), ErrorCode.DATA_CONFLICT.name()).contains(outcomes.get(0)),
                    "the other confirm must be rejected with 409: " + outcomes);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        assertEquals(ProposalStatus.CONFIRMED, proposalRepository.findById(proposal.proposalId()).orElseThrow().getStatus());
        assertEquals(v0 + 1, storedVersion(proposal.proposalId()));
    }
}
