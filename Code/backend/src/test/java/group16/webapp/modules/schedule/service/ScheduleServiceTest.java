package group16.webapp.modules.schedule.service;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.assignment.api.AssignmentApi;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.availability.api.AvailabilityApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.notification.api.NotificationApi;
import group16.webapp.modules.schedule.api.ProposalStatus;
import group16.webapp.modules.schedule.api.SessionStatus;
import group16.webapp.modules.schedule.domain.ScheduleProposal;
import group16.webapp.modules.schedule.domain.ScheduleSession;
import group16.webapp.modules.schedule.dto.ConfirmRequest;
import group16.webapp.modules.schedule.dto.GenerateScheduleRequest;
import group16.webapp.modules.schedule.dto.SessionEditRequest;
import group16.webapp.modules.schedule.engine.ConstraintValidator;
import group16.webapp.modules.schedule.engine.SchedulingEngine;
import group16.webapp.modules.schedule.repository.ScheduleProposalRepository;
import group16.webapp.modules.schedule.repository.ScheduleSessionRepository;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.api.TaskSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ScheduleServiceTest {

    @Mock private ScheduleProposalRepository proposalRepository;
    @Mock private ScheduleSessionRepository sessionRepository;
    @Mock private AvailabilityApi availabilityApi;
    @Mock private TaskApi taskApi;
    @Mock private AssignmentApi assignmentApi;
    @Mock private UserApi userApi;
    @Mock private NotificationApi notificationApi;

    @Mock private ObjectProvider<AvailabilityApi> availabilityProvider;
    @Mock private ObjectProvider<TaskApi> taskProvider;
    @Mock private ObjectProvider<AssignmentApi> assignmentProvider;
    @Mock private ObjectProvider<UserApi> userProvider;
    @Mock private ObjectProvider<NotificationApi> notificationProvider;

    private ScheduleService service;

    @BeforeEach
    void setUp() {
        lenient().when(availabilityProvider.getIfAvailable()).thenReturn(availabilityApi);
        lenient().when(taskProvider.getIfAvailable()).thenReturn(taskApi);
        lenient().when(assignmentProvider.getIfAvailable()).thenReturn(assignmentApi);
        lenient().when(userProvider.getIfAvailable()).thenReturn(userApi);
        lenient().when(notificationProvider.getIfAvailable()).thenReturn(notificationApi);

        service = new ScheduleService(
                proposalRepository, sessionRepository,
                availabilityProvider, taskProvider, assignmentProvider, userProvider, notificationProvider,
                new SchedulingEngine(), new ConstraintValidator()
        );
    }

    @Test
    void generateProposal_throwsNoAvailabilityRuleWhenUserHasNone() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(24, ChronoUnit.HOURS);
        when(availabilityApi.hasActiveRule(1L)).thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> service.generateProposal(1L, new GenerateScheduleRequest(start, end, null)));
        assertEquals(ErrorCode.NO_AVAILABILITY_RULE, ex.getErrorCode());
    }

    @Test
    void editSession_throwsStaleVersionWhenBatchVersionMismatches() {
        ScheduleSession session = new ScheduleSession();
        org.springframework.test.util.ReflectionTestUtils.setField(session, "id", 200L); session.setProposalId(100L);
        session.setUserId(1L);
        session.setStatus(SessionStatus.PROPOSED);

        ScheduleProposal proposal = new ScheduleProposal();
        org.springframework.test.util.ReflectionTestUtils.setField(proposal, "id", 100L); proposal.setUserId(1L);
        proposal.setStatus(ProposalStatus.PROPOSED);
        proposal.setVersion(3);

        when(sessionRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(session));
        when(proposalRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(proposal));

        Instant s = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant e = s.plus(1, ChronoUnit.HOURS);

        AppException ex = assertThrows(AppException.class,
                () -> service.editSession(1L, 50L, new SessionEditRequest(s, e, 2)));
        assertEquals(ErrorCode.STALE_VERSION, ex.getErrorCode());
    }

    @Test
    void confirmProposal_confirmsSessionsWhenAllConstraintsPass() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        ScheduleProposal proposal = new ScheduleProposal();
        org.springframework.test.util.ReflectionTestUtils.setField(proposal, "id", 100L); proposal.setUserId(1L);
        proposal.setStatus(ProposalStatus.PROPOSED);
        proposal.setVersion(1);
        proposal.setHorizonStart(start.minus(1, ChronoUnit.HOURS));
        proposal.setHorizonEnd(end.plus(5, ChronoUnit.HOURS));

        ScheduleSession session = new ScheduleSession();
        org.springframework.test.util.ReflectionTestUtils.setField(session, "id", 200L); session.setProposalId(100L);
        session.setTaskId(10L);
        session.setUserId(1L);
        session.setStartAt(start);
        session.setEndAt(end);
        session.setStatus(SessionStatus.PROPOSED);

        when(proposalRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(proposal));
        when(sessionRepository.findByProposalIdOrderByStartAtAsc(100L)).thenReturn(List.of(session));
        when(availabilityApi.availableIntervals(eq(1L), any(), any())).thenReturn(List.of(new TimeRange(start, end)));
        when(availabilityApi.fixedEventOccurrences(eq(1L), any(), any())).thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(eq(1L), any(), any(), any()))
                .thenReturn(List.of());
        when(assignmentApi.acceptedTaskIds(1L)).thenReturn(Set.of());
        when(taskApi.schedulableTasks(1L, Set.of())).thenReturn(List.of(
                new SchedulableTask(10L, end.plus(2, ChronoUnit.HOURS), TaskPriority.HIGH, 60, Instant.now())
        ));
        when(sessionRepository.findByUserIdAndStatusIn(eq(1L), any())).thenReturn(List.of());
        when(taskApi.summaries(anyList())).thenReturn(List.of(
                new TaskSummary(10L, null, "Algorithm Lab", end.plus(2, ChronoUnit.HOURS), TaskPriority.HIGH, TaskStatus.TODO)
        ));
        when(sessionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var res = service.confirmProposal(1L, 100L, new ConfirmRequest(1));
        assertEquals(ProposalStatus.CONFIRMED, res.status());
        assertEquals(2, res.version());
        assertNotNull(res.confirmedAt());
        assertEquals(SessionStatus.CONFIRMED, res.sessions().get(0).status());
    }
}