package group16.webapp.modules.assignment.service.impl;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.assignment.api.AssignmentApi;
import group16.webapp.modules.assignment.api.AssignmentStatus;
import group16.webapp.modules.assignment.dto.AssignmentCreateRequest;
import group16.webapp.modules.assignment.dto.AssignmentResponse;
import group16.webapp.modules.assignment.dto.AssignmentTaskSummary;
import group16.webapp.modules.assignment.entity.TaskAssignment;
import group16.webapp.modules.assignment.repository.TaskAssignmentRepository;
import group16.webapp.modules.assignment.service.AssignmentService;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.group.api.GroupAccessApi;
import group16.webapp.modules.task.api.NewGroupTaskCommand;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskCategory;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AS-01/AS-02. Leader rights are checked with {@link GroupAccessApi#requireLeader} on every request (D16; the
 * check-then-write race is accepted, O12a). The group is always derived from the task (task_assignments has no
 * group_id). BR-04 (one PENDING/ACCEPTED assignment per task) is checked first and backed by the V8 partial
 * unique index for concurrent creates.
 */
@Service
public class AssignmentServiceImpl implements AssignmentService, AssignmentApi {

    private static final Set<AssignmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AssignmentStatus.PENDING, AssignmentStatus.ACCEPTED);

    private final TaskAssignmentRepository repository;
    private final TaskApi taskApi;
    private final GroupAccessApi groupAccessApi;
    private final UserApi userApi;
    private final Clock clock;

    @Autowired
    public AssignmentServiceImpl(TaskAssignmentRepository repository, TaskApi taskApi,
                                 GroupAccessApi groupAccessApi, UserApi userApi) {
        this(repository, taskApi, groupAccessApi, userApi, Clock.systemUTC());
    }

    public AssignmentServiceImpl(TaskAssignmentRepository repository, TaskApi taskApi,
                                 GroupAccessApi groupAccessApi, UserApi userApi, Clock clock) {
        this.repository = repository;
        this.taskApi = taskApi;
        this.groupAccessApi = groupAccessApi;
        this.userApi = userApi;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long callerId, AssignmentStatus status, Long groupId) {
        requireActiveCaller(callerId);
        List<TaskAssignment> assignments;
        if (groupId == null) {
            assignments = status == null
                    ? repository.findByAssigneeUserIdOrderByCreatedAtDescIdDesc(callerId)
                    : repository.findByAssigneeUserIdAndStatusOrderByCreatedAtDescIdDesc(callerId, status);
        } else {
            groupAccessApi.requireLeader(groupId, callerId);
            List<Long> taskIds = taskApi.groupTaskIds(groupId);
            if (taskIds.isEmpty()) {
                return List.of();
            }
            assignments = status == null
                    ? repository.findByTaskIdInOrderByCreatedAtDescIdDesc(taskIds)
                    : repository.findByTaskIdInAndStatusOrderByCreatedAtDescIdDesc(taskIds, status);
        }
        if (assignments.isEmpty()) {
            return List.of();
        }

        Set<Long> taskIds = new HashSet<>();
        assignments.forEach(a -> taskIds.add(a.getTaskId()));
        Map<Long, TaskSummary> summaries = taskApi.summaries(taskIds).stream()
                .collect(Collectors.toMap(TaskSummary::taskId, Function.identity()));
        // Assignments of soft-deleted tasks are left out (summaries skips deleted tasks).
        return assignments.stream()
                .filter(a -> summaries.containsKey(a.getTaskId()))
                .map(a -> toResponse(a, summaries.get(a.getTaskId())))
                .toList();
    }

    @Override
    @Transactional
    public AssignmentResponse create(Long callerId, AssignmentCreateRequest request) {
        requireActiveCaller(callerId);
        Long taskId;
        if (request.newTask() != null) {
            AssignmentCreateRequest.NewGroupTask newTask = request.newTask();
            groupAccessApi.requireLeader(newTask.groupId(), callerId);
            requireAssignableMember(newTask.groupId(), request.assigneeUserId());
            taskId = taskApi.createGroupTask(newTask.groupId(), callerId, new NewGroupTaskCommand(
                    newTask.title(),
                    newTask.description(),
                    newTask.deadline(),
                    TaskPriority.valueOf(newTask.priority()),
                    TaskCategory.valueOf(newTask.category()),
                    newTask.estimatedDurationMinutes()));
        } else {
            taskId = request.taskId();
            // Personal, deleted and missing tasks give the same 404 (TV1 decision 2026-11-19).
            Long groupId = taskApi.findActiveGroupTaskGroupId(taskId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
            groupAccessApi.requireLeader(groupId, callerId);
            requireAssignableMember(groupId, request.assigneeUserId());
            if (repository.existsByTaskIdAndStatusIn(taskId, ACTIVE_STATUSES)) {
                throw new AppException(ErrorCode.ASSIGNMENT_ALREADY_ACTIVE);
            }
        }

        TaskAssignment saved;
        try {
            saved = repository.saveAndFlush(new TaskAssignment(taskId, request.assigneeUserId(), callerId));
        } catch (DataIntegrityViolationException e) {
            // A concurrent create won the V8 partial unique index (BR-04).
            throw new AppException(ErrorCode.ASSIGNMENT_ALREADY_ACTIVE);
        }
        return toResponse(saved, summaryOf(taskId));
    }

    @Override
    @Transactional
    public AssignmentResponse accept(Long callerId, Long assignmentId) {
        requireActiveCaller(callerId);
        requireAssignee(callerId, assignmentId);
        if (repository.accept(assignmentId, callerId, now()) == 0) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
        return reload(assignmentId);
    }

    @Override
    @Transactional
    public AssignmentResponse decline(Long callerId, Long assignmentId, String reason) {
        requireActiveCaller(callerId);
        requireAssignee(callerId, assignmentId);
        if (repository.decline(assignmentId, callerId, reason.strip(), now()) == 0) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
        return reload(assignmentId);
    }

    @Override
    @Transactional
    public AssignmentResponse revoke(Long callerId, Long assignmentId) {
        requireActiveCaller(callerId);
        TaskAssignment assignment = find(assignmentId);
        Long groupId = taskApi.findActiveGroupTaskGroupId(assignment.getTaskId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        // The current LEADER, not assigned_by_user_id (D16: after a transfer the former LEADER is a MEMBER).
        groupAccessApi.requireLeader(groupId, callerId);
        if (repository.revoke(assignmentId, now()) == 0) {
            throw new AppException(ErrorCode.DATA_CONFLICT);
        }
        return reload(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> acceptedTaskIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return Set.copyOf(repository.findAcceptedTaskIds(userId));
    }

    private void requireActiveCaller(Long callerId) {
        userApi.findById(callerId)
                .filter(UserSummary::active)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }

    /** The assignee must be an ACTIVE member (leaders included) with an active account; else 400 INVALID_INPUT. */
    private void requireAssignableMember(Long groupId, Long assigneeUserId) {
        boolean activeAccount = userApi.findById(assigneeUserId).map(UserSummary::active).orElse(false);
        if (!activeAccount || !groupAccessApi.isActiveMember(groupId, assigneeUserId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }

    private void requireAssignee(Long callerId, Long assignmentId) {
        if (!find(assignmentId).getAssigneeUserId().equals(callerId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private TaskAssignment find(Long assignmentId) {
        return repository.findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private AssignmentResponse reload(Long assignmentId) {
        TaskAssignment assignment = find(assignmentId);
        return toResponse(assignment, summaryOf(assignment.getTaskId()));
    }

    private TaskSummary summaryOf(Long taskId) {
        return taskApi.summaries(List.of(taskId)).stream().findFirst().orElse(null);
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private static AssignmentResponse toResponse(TaskAssignment a, TaskSummary task) {
        AssignmentTaskSummary summary = task == null ? null : new AssignmentTaskSummary(
                task.taskId(),
                task.title(),
                task.deadline(),
                task.priority() == null ? null : task.priority().name(),
                task.status() == null ? null : task.status().name());
        return new AssignmentResponse(
                a.getId(),
                a.getStatus(),
                a.getAssigneeUserId(),
                a.getAssignedByUserId(),
                a.getDeclineReason(),
                a.getRespondedAt(),
                a.getCreatedAt(),
                summary);
    }
}
