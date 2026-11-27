package group16.webapp.modules.schedule.service;

import group16.webapp.common.dto.ConflictErrorDto;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.modules.assignment.api.AssignmentApi;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.AvailabilityApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.notification.api.NotificationApi;
import group16.webapp.modules.notification.api.NotificationType;
import group16.webapp.modules.schedule.api.*;
import group16.webapp.modules.schedule.domain.ScheduleProposal;
import group16.webapp.modules.schedule.domain.ScheduleSession;
import group16.webapp.modules.schedule.dto.ConfirmRequest;
import group16.webapp.modules.schedule.dto.GenerateScheduleRequest;
import group16.webapp.modules.schedule.dto.ProposalBatchResponse;
import group16.webapp.modules.schedule.dto.SessionEditRequest;
import group16.webapp.modules.schedule.engine.ConstraintValidator;
import group16.webapp.modules.schedule.engine.SchedulingEngine;
import group16.webapp.modules.schedule.engine.SlotCalculator;
import group16.webapp.modules.schedule.repository.ScheduleProposalRepository;
import group16.webapp.modules.schedule.repository.ScheduleSessionRepository;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskSummary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScheduleService implements ScheduleApi {

    private static final List<SessionStatus> COMMITTED_STATUSES = List.of(SessionStatus.CONFIRMED, SessionStatus.COMPLETED);

    private final ScheduleProposalRepository proposalRepository;
    private final ScheduleSessionRepository sessionRepository;
    private final ObjectProvider<AvailabilityApi> availabilityApiProvider;
    private final ObjectProvider<TaskApi> taskApiProvider;
    private final ObjectProvider<AssignmentApi> assignmentApiProvider;
    private final ObjectProvider<UserApi> userApiProvider;
    private final ObjectProvider<NotificationApi> notificationApiProvider;
    private final SchedulingEngine schedulingEngine;
    private final ConstraintValidator constraintValidator;

    public ScheduleService(
            ScheduleProposalRepository proposalRepository,
            ScheduleSessionRepository sessionRepository,
            ObjectProvider<AvailabilityApi> availabilityApiProvider,
            ObjectProvider<TaskApi> taskApiProvider,
            ObjectProvider<AssignmentApi> assignmentApiProvider,
            ObjectProvider<UserApi> userApiProvider,
            ObjectProvider<NotificationApi> notificationApiProvider,
            SchedulingEngine schedulingEngine,
            ConstraintValidator constraintValidator
    ) {
        this.proposalRepository = proposalRepository;
        this.sessionRepository = sessionRepository;
        this.availabilityApiProvider = availabilityApiProvider;
        this.taskApiProvider = taskApiProvider;
        this.assignmentApiProvider = assignmentApiProvider;
        this.userApiProvider = userApiProvider;
        this.notificationApiProvider = notificationApiProvider;
        this.schedulingEngine = schedulingEngine;
        this.constraintValidator = constraintValidator;
    }

    @Transactional
    public ProposalBatchResponse generateProposal(Long userId, GenerateScheduleRequest request) {
        if (!request.windowStart().isBefore(request.windowEnd())) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }
        AvailabilityApi availabilityApi = availabilityApiProvider.getIfAvailable();
        if (availabilityApi == null || !availabilityApi.hasActiveRule(userId)) {
            throw new AppException(ErrorCode.NO_AVAILABILITY_RULE);
        }

        Instant now = Instant.now();

        proposalRepository.findByUserIdAndStatus(userId, ProposalStatus.PROPOSED).ifPresent(existing -> {
            existing.setStatus(ProposalStatus.CANCELLED);
            proposalRepository.saveAndFlush(existing);
            List<ScheduleSession> oldSessions = sessionRepository.findByProposalIdOrderByStartAtAsc(existing.getId());
            for (ScheduleSession s : oldSessions) {
                if (s.getStatus() == SessionStatus.PROPOSED) {
                    s.setStatus(SessionStatus.CANCELLED);
                }
            }
            sessionRepository.saveAllAndFlush(oldSessions);
        });

        AssignmentApi assignmentApi = assignmentApiProvider.getIfAvailable();
        Set<Long> acceptedGroupTaskIds = assignmentApi != null ? assignmentApi.acceptedTaskIds(userId) : Set.of();

        TaskApi taskApi = taskApiProvider.getIfAvailable();
        List<SchedulableTask> allSchedulable = taskApi != null ? taskApi.schedulableTasks(userId, acceptedGroupTaskIds) : List.of();

        List<SchedulableTask> targetTasks = allSchedulable;
        if (request.includeTaskIds() != null && !request.includeTaskIds().isEmpty()) {
            Set<Long> filter = new HashSet<>(request.includeTaskIds());
            targetTasks = allSchedulable.stream()
                    .filter(t -> filter.contains(t.taskId()))
                    .toList();
        }

        Map<Long, Integer> remainingByTask = computeRemainingMinutes(userId, targetTasks);

        List<TimeRange> availIntervals = availabilityApi.availableIntervals(userId, request.windowStart(), request.windowEnd());
        List<TimeRange> fixedEvents = availabilityApi.fixedEventOccurrences(userId, request.windowStart(), request.windowEnd());
        List<TimeRange> committedBusy = sessionRepository
                .findByUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                        userId, COMMITTED_STATUSES, request.windowEnd(), request.windowStart())
                .stream()
                .map(s -> new TimeRange(s.getStartAt(), s.getEndAt()))
                .toList();

        List<TimeRange> allBusy = new ArrayList<>(fixedEvents);
        allBusy.addAll(committedBusy);
        List<TimeRange> freeSlots = SlotCalculator.subtractBusy(availIntervals, allBusy);

        SchedulingEngine.EngineResult result = schedulingEngine.generate(targetTasks, remainingByTask, freeSlots, now);

        ZoneId userZone = resolveUserZone(userId);
        LocalDate weekStart = LocalDate.ofInstant(request.windowStart(), userZone)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        ScheduleProposal proposal = new ScheduleProposal();
        proposal.setUserId(userId);
        proposal.setWeekStart(weekStart);
        proposal.setHorizonStart(request.windowStart());
        proposal.setHorizonEnd(request.windowEnd());
        proposal.setSource(ProposalSource.ENGINE);
        proposal.setStatus(ProposalStatus.PROPOSED);
        proposal = proposalRepository.saveAndFlush(proposal);

        List<ScheduleSession> savedSessions = new ArrayList<>();
        for (SchedulingEngine.PlannedSlot slot : result.slots()) {
            ScheduleSession session = new ScheduleSession();
            session.setProposalId(proposal.getId());
            session.setTaskId(slot.taskId());
            session.setUserId(userId);
            session.setStartAt(slot.startAt());
            session.setEndAt(slot.endAt());
            session.setStatus(SessionStatus.PROPOSED);
            savedSessions.add(session);
        }
        savedSessions = sessionRepository.saveAll(savedSessions);

        NotificationApi notificationApi = notificationApiProvider.getIfAvailable();
        if (notificationApi != null) {
            notificationApi.notify(userId, NotificationType.SCHEDULE_PROPOSAL_READY,
                    "Smart schedule proposal ready",
                    "Generated " + savedSessions.size() + " study/work sessions.",
                    "SCHEDULE_PROPOSAL", proposal.getId());
        }

        Map<Long, String> titleMap = fetchTaskTitles(targetTasks.stream().map(SchedulableTask::taskId).toList());
        List<ProposalBatchResponse.UnscheduledTask> unscheduledDtos = result.unscheduled().stream()
                .map(u -> new ProposalBatchResponse.UnscheduledTask(
                        u.taskId(),
                        titleMap.getOrDefault(u.taskId(), "Task #" + u.taskId()),
                        u.reason(),
                        u.missingMinutes()))
                .toList();

        return toBatchResponse(proposal, savedSessions, unscheduledDtos, titleMap);
    }

    @Transactional(readOnly = true)
    public ProposalBatchResponse getProposalById(Long userId, Long proposalId) {
        ScheduleProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        List<ScheduleSession> sessions = sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.getId());
        Map<Long, String> titles = fetchTaskTitles(sessions.stream().map(ScheduleSession::getTaskId).toList());
        return toBatchResponse(proposal, sessions, List.of(), titles);
    }

    @Transactional(readOnly = true)
    public Optional<ProposalBatchResponse> getCurrentProposal(Long userId) {
        return proposalRepository.findByUserIdAndStatus(userId, ProposalStatus.PROPOSED)
                .map(proposal -> {
                    List<ScheduleSession> sessions = sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.getId());
                    Map<Long, String> titles = fetchTaskTitles(sessions.stream().map(ScheduleSession::getTaskId).toList());
                    return toBatchResponse(proposal, sessions, List.of(), titles);
                });
    }

    @Transactional
    public ProposalBatchResponse editSession(Long userId, Long sessionId, SessionEditRequest request) {
        // HC-04: startAt < endAt and the same calendar day in the user's timezone (request-level 400).
        if (!SlotCalculator.isWithinOneLocalDay(request.startAt(), request.endAt(), resolveUserZone(userId))) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }

        ScheduleSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (session.getStatus() != SessionStatus.PROPOSED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Only PROPOSED sessions can be edited");
        }

        ScheduleProposal proposal = proposalRepository.findByIdAndUserId(session.getProposalId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (proposal.getStatus() != ProposalStatus.PROPOSED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Proposal batch is no longer active");
        }

        verifyAndBumpBatchVersion(proposal, request.version());

        // Siblings are read before the edit is applied; the edited session goes last so that cumulative checks
        // (HC-07) are attributed to it. Only the edited session's conflicts block the edit (BR-10).
        List<ScheduleSession> validationOrder = new ArrayList<>(
                sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.getId()).stream()
                        .filter(s -> !Objects.equals(s.getId(), session.getId()))
                        .toList());
        session.setStartAt(request.startAt());
        session.setEndAt(request.endAt());
        validationOrder.add(session);

        List<ConflictErrorDto> conflicts = checkHardConstraints(userId, validationOrder, Instant.now()).conflicts().stream()
                .filter(c -> Objects.equals(c.sessionId(), session.getId()))
                .toList();
        if (!conflicts.isEmpty()) {
            // Thrown inside the transaction: the version bump and the new times are rolled back.
            throw AppException.conflicts(conflicts);
        }

        session.setVersion(session.getVersion() + 1);
        sessionRepository.save(session);

        List<ScheduleSession> allSessions = sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.getId());
        Map<Long, String> titles = fetchTaskTitles(allSessions.stream().map(ScheduleSession::getTaskId).toList());
        return toBatchResponse(proposal, allSessions, List.of(), titles);
    }

    @Transactional
    public ProposalBatchResponse deleteSession(Long userId, Long sessionId, int expectedBatchVersion) {
        if (expectedBatchVersion < 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        ScheduleSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (session.getStatus() != SessionStatus.PROPOSED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Only PROPOSED sessions can be deleted");
        }

        ScheduleProposal proposal = proposalRepository.findByIdAndUserId(session.getProposalId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (proposal.getStatus() != ProposalStatus.PROPOSED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Proposal batch is no longer active");
        }

        verifyAndBumpBatchVersion(proposal, expectedBatchVersion);

        sessionRepository.delete(session);
        sessionRepository.flush();

        List<ScheduleSession> remaining = sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.getId());
        Map<Long, String> titles = fetchTaskTitles(remaining.stream().map(ScheduleSession::getTaskId).toList());
        return toBatchResponse(proposal, remaining, List.of(), titles);
    }

    @Transactional
    public void discardProposal(Long userId, Long proposalId, int expectedVersion) {
        ScheduleProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (proposal.getStatus() != ProposalStatus.PROPOSED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Proposal is already finalized");
        }

        verifyAndBumpBatchVersion(proposal, expectedVersion);

        proposal.setStatus(ProposalStatus.CANCELLED);
        proposalRepository.save(proposal);

        List<ScheduleSession> sessions = sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.getId());
        for (ScheduleSession s : sessions) {
            if (s.getStatus() == SessionStatus.PROPOSED) {
                s.setStatus(SessionStatus.CANCELLED);
            }
        }
        sessionRepository.saveAll(sessions);
    }

    @Transactional
    public ProposalBatchResponse confirmProposal(Long userId, Long proposalId, ConfirmRequest request) {
        ScheduleProposal proposal = proposalRepository.findByIdAndUserId(proposalId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        if (proposal.getStatus() != ProposalStatus.PROPOSED) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Only PROPOSED batches can be confirmed");
        }

        verifyAndBumpBatchVersion(proposal, request.version());

        List<ScheduleSession> sessions = sessionRepository.findByProposalIdOrderByStartAtAsc(proposal.getId());
        if (sessions.isEmpty()) {
            throw new AppException(ErrorCode.DATA_CONFLICT, "Cannot confirm an empty schedule proposal");
        }

        HardConstraintCheck check = checkHardConstraints(userId, sessions, Instant.now());
        if (!check.conflicts().isEmpty()) {
            throw AppException.conflicts(check.conflicts());
        }
        Map<Long, TaskSummary> summaryMap = check.summaries();

        Instant confirmedAt = Instant.now();
        proposal.setStatus(ProposalStatus.CONFIRMED);
        proposal.setConfirmedAt(confirmedAt);
        proposalRepository.save(proposal);

        for (ScheduleSession s : sessions) {
            s.setStatus(SessionStatus.CONFIRMED);
        }
        sessions = sessionRepository.saveAll(sessions);

        Map<Long, String> titleMap = summaryMap.values().stream()
                .collect(Collectors.toMap(TaskSummary::taskId, TaskSummary::title));

        return toBatchResponse(proposal, sessions, List.of(), titleMap);
    }

    @Transactional(readOnly = true)
    public List<ProposalBatchResponse.ProposedSession> getCalendarSessions(Long userId, Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }
        List<ScheduleSession> sessions = sessionRepository
                .findByUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(userId, COMMITTED_STATUSES, end, start);
        Map<Long, String> titles = fetchTaskTitles(sessions.stream().map(ScheduleSession::getTaskId).toList());
        return sessions.stream()
                .sorted(Comparator.comparing(ScheduleSession::getStartAt))
                .map(s -> new ProposalBatchResponse.ProposedSession(
                        s.getId(),
                        s.getTaskId(),
                        titles.getOrDefault(s.getTaskId(), "Task #" + s.getTaskId()),
                        s.getStartAt(),
                        s.getEndAt(),
                        s.getStatus(),
                        s.getVersion()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledSession> committedSessions(Long userId, Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            return List.of();
        }
        return sessionRepository
                .findByUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(userId, COMMITTED_STATUSES, end, start)
                .stream()
                .map(s -> new ScheduledSession(s.getId(), s.getTaskId(), s.getStartAt(), s.getEndAt(), s.getStatus()))
                .toList();
    }

    private record HardConstraintCheck(List<ConflictErrorDto> conflicts, Map<Long, TaskSummary> summaries) {}

    /**
     * Re-reads availability, fixed events, committed sessions and task eligibility, then checks HC-01..HC-08 (HC-04 in the
     * user's current timezone) for {@code sessions} (shared by edit, BR-10, and confirm, BR-11).
     */
    private HardConstraintCheck checkHardConstraints(Long userId, List<ScheduleSession> sessions, Instant now) {
        Instant minStart = sessions.stream().map(ScheduleSession::getStartAt).min(Instant::compareTo).orElseThrow();
        Instant maxEnd = sessions.stream().map(ScheduleSession::getEndAt).max(Instant::compareTo).orElseThrow();

        AvailabilityApi availabilityApi = availabilityApiProvider.getIfAvailable();
        List<TimeRange> availIntervals = availabilityApi != null ? availabilityApi.availableIntervals(userId, minStart, maxEnd) : List.of();
        List<TimeRange> fixedEvents = availabilityApi != null ? availabilityApi.fixedEventOccurrences(userId, minStart, maxEnd) : List.of();
        List<TimeRange> existingCommitted = sessionRepository
                .findByUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(userId, COMMITTED_STATUSES, maxEnd, minStart)
                .stream()
                .map(s -> new TimeRange(s.getStartAt(), s.getEndAt()))
                .toList();

        AssignmentApi assignmentApi = assignmentApiProvider.getIfAvailable();
        Set<Long> acceptedGroupIds = assignmentApi != null ? assignmentApi.acceptedTaskIds(userId) : Set.of();

        TaskApi taskApi = taskApiProvider.getIfAvailable();
        List<SchedulableTask> schedulableTasks = taskApi != null ? taskApi.schedulableTasks(userId, acceptedGroupIds) : List.of();
        Set<Long> schedulableTaskIds = schedulableTasks.stream().map(SchedulableTask::taskId).collect(Collectors.toSet());
        Map<Long, Integer> remainingMinutes = computeRemainingMinutes(userId, schedulableTasks);

        List<Long> taskIds = sessions.stream().map(ScheduleSession::getTaskId).distinct().toList();
        Map<Long, TaskSummary> summaryMap = taskApi != null
                ? taskApi.summaries(taskIds).stream().collect(Collectors.toMap(TaskSummary::taskId, Function.identity()))
                : Map.of();

        List<ConflictErrorDto> conflicts = constraintValidator.validateBatch(
                sessions, availIntervals, fixedEvents, existingCommitted,
                summaryMap, remainingMinutes, schedulableTaskIds, resolveUserZone(userId), now
        );
        return new HardConstraintCheck(conflicts, summaryMap);
    }

    private void verifyAndBumpBatchVersion(ScheduleProposal proposal, Integer expectedVersion) {
        if (expectedVersion == null || !Objects.equals(proposal.getVersion(), expectedVersion)) {
            throw new AppException(ErrorCode.STALE_VERSION);
        }
        proposal.setVersion(proposal.getVersion() + 1);
    }

    private Map<Long, Integer> computeRemainingMinutes(Long userId, List<SchedulableTask> tasks) {
        Map<Long, Integer> committedMinutesByTask = new HashMap<>();
        for (ScheduleSession s : sessionRepository.findByUserIdAndStatusIn(userId, COMMITTED_STATUSES)) {
            int mins = SlotCalculator.minutesBetween(s.getStartAt(), s.getEndAt());
            committedMinutesByTask.merge(s.getTaskId(), mins, Integer::sum);
        }

        Map<Long, Integer> remaining = new HashMap<>();
        for (SchedulableTask t : tasks) {
            int alreadyScheduled = committedMinutesByTask.getOrDefault(t.taskId(), 0);
            remaining.put(t.taskId(), Math.max(0, t.estimatedDurationMinutes() - alreadyScheduled));
        }
        return remaining;
    }

    private Map<Long, String> fetchTaskTitles(Collection<Long> taskIds) {
        TaskApi taskApi = taskApiProvider.getIfAvailable();
        if (taskApi == null || taskIds == null || taskIds.isEmpty()) return Map.of();
        return taskApi.summaries(new HashSet<>(taskIds)).stream()
                .collect(Collectors.toMap(TaskSummary::taskId, TaskSummary::title, (a, b) -> a));
    }

    private ZoneId resolveUserZone(Long userId) {
        UserApi userApi = userApiProvider.getIfAvailable();
        if (userApi == null) return ZoneId.of(DateUtils.DEFAULT_TIMEZONE);
        return userApi.findById(userId)
                .map(UserSummary::timezone)
                .filter(DateUtils::isValidTimezone)
                .map(ZoneId::of)
                .orElse(ZoneId.of(DateUtils.DEFAULT_TIMEZONE));
    }

    private ProposalBatchResponse toBatchResponse(
            ScheduleProposal proposal,
            List<ScheduleSession> sessions,
            List<ProposalBatchResponse.UnscheduledTask> unscheduled,
            Map<Long, String> titleMap
    ) {
        List<ProposalBatchResponse.ProposedSession> sessionDtos = sessions.stream()
                .map(s -> new ProposalBatchResponse.ProposedSession(
                        s.getId(),
                        s.getTaskId(),
                        titleMap.getOrDefault(s.getTaskId(), "Task #" + s.getTaskId()),
                        s.getStartAt(),
                        s.getEndAt(),
                        s.getStatus(),
                        s.getVersion()))
                .toList();

        return new ProposalBatchResponse(
                proposal.getId(),
                proposal.getVersion(),
                proposal.getStatus(),
                proposal.getHorizonStart(),
                proposal.getHorizonEnd(),
                sessionDtos,
                unscheduled,
                proposal.getCreatedAt(),
                proposal.getConfirmedAt()
        );
    }
}