package group16.webapp.modules.task.service;

import group16.webapp.common.dto.PageResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.task.api.NewGroupTaskCommand;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.api.TaskSummary;
import group16.webapp.modules.task.dto.TaskCreateRequest;
import group16.webapp.modules.task.dto.TaskResponse;
import group16.webapp.modules.task.dto.TaskStatusRequest;
import group16.webapp.modules.task.dto.TaskUpdateRequest;
import group16.webapp.modules.task.entity.TaskEntity;
import group16.webapp.modules.task.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class TaskService implements TaskApi {

    private static final Set<TaskStatus> SCHEDULABLE_STATUSES =
            EnumSet.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

    private final TaskRepository taskRepository;
    private final Clock clock;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this(taskRepository, Clock.systemUTC());
    }

    public TaskService(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(
            Long userId,
            TaskStatus status,
            TaskPriority priority,
            Instant deadlineFrom,
            Instant deadlineTo,
            int page,
            int size) {
        if (deadlineFrom != null && deadlineTo != null && deadlineFrom.isAfter(deadlineTo)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }

        Specification<TaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerUserId"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (deadlineFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deadline"), deadlineFrom));
            }
            if (deadlineTo != null) {
                predicates.add(cb.lessThan(root.get("deadline"), deadlineTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Instant now = Instant.now(clock);
        Page<TaskResponse> result = taskRepository.findAll(spec, pageable)
                .map(entity -> toResponse(entity, now));
        return PageResponse.from(result);
    }

    @Transactional
    public TaskResponse createTask(Long userId, TaskCreateRequest request) {
        TaskEntity entity = new TaskEntity();
        entity.setOwnerUserId(userId);
        entity.setGroupId(null);
        entity.setTitle(request.title().trim());
        entity.setDescription(request.description() == null ? "" : request.description());
        entity.setDeadline(request.deadline());
        entity.setPriority(request.priority());
        entity.setCategory(request.category());
        entity.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        entity.setStatus(TaskStatus.TODO);

        TaskEntity saved = taskRepository.saveAndFlush(entity);
        return toResponse(saved, Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long userId, Long taskId) {
        TaskEntity entity = findOwnedActiveTask(userId, taskId);
        return toResponse(entity, Instant.now(clock));
    }

    @Transactional
    public TaskResponse updateTask(Long userId, Long taskId, TaskUpdateRequest request) {
        TaskEntity entity = findOwnedActiveTask(userId, taskId);
        checkVersion(entity, request.version());

        entity.setTitle(request.title().trim());
        entity.setDescription(request.description() == null ? "" : request.description());
        entity.setDeadline(request.deadline());
        entity.setPriority(request.priority());
        entity.setCategory(request.category());
        entity.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        entity.setActualDurationMinutes(request.actualDurationMinutes());

        TaskEntity saved = taskRepository.saveAndFlush(entity);
        return toResponse(saved, Instant.now(clock));
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        TaskEntity entity = findOwnedActiveTask(userId, taskId);
        entity.setDeletedAt(Instant.now(clock));
        taskRepository.saveAndFlush(entity);
    }

    @Transactional
    public TaskResponse changeStatus(Long userId, Long taskId, TaskStatusRequest request) {
        TaskEntity entity = findOwnedActiveTask(userId, taskId);
        checkVersion(entity, request.version());

        entity.setStatus(request.status());
        TaskEntity saved = taskRepository.saveAndFlush(entity);
        return toResponse(saved, Instant.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchedulableTask> schedulableTasks(Long userId, Collection<Long> acceptedGroupTaskIds) {
        List<TaskEntity> entities;
        if (acceptedGroupTaskIds == null || acceptedGroupTaskIds.isEmpty()) {
            entities = taskRepository.findPersonalSchedulable(userId, SCHEDULABLE_STATUSES);
        } else {
            entities = taskRepository.findSchedulableWithGroupTasks(
                    userId, acceptedGroupTaskIds, SCHEDULABLE_STATUSES);
        }
        return entities.stream()
                .map(t -> new SchedulableTask(
                        t.getId(),
                        t.getDeadline(),
                        t.getPriority(),
                        t.getEstimatedDurationMinutes(),
                        t.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public Long createGroupTask(Long groupId, Long creatorUserId, NewGroupTaskCommand command) {
        TaskEntity entity = new TaskEntity();
        entity.setOwnerUserId(creatorUserId);
        entity.setGroupId(groupId);
        entity.setTitle(command.title().trim());
        entity.setDescription(command.description() == null ? "" : command.description());
        entity.setDeadline(command.deadline());
        entity.setPriority(command.priority());
        entity.setCategory(command.category());
        entity.setEstimatedDurationMinutes(command.estimatedDurationMinutes());
        entity.setStatus(TaskStatus.TODO);

        return taskRepository.saveAndFlush(entity).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSummary> summaries(Collection<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        return taskRepository.findByIdInAndDeletedAtIsNull(taskIds).stream()
                .map(t -> new TaskSummary(
                        t.getId(),
                        t.getGroupId(),
                        t.getTitle(),
                        t.getDeadline(),
                        t.getPriority(),
                        t.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findActiveGroupTaskGroupId(Long taskId) {
        if (taskId == null) {
            return Optional.empty();
        }
        return taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .map(TaskEntity::getGroupId);
    }

    private TaskEntity findOwnedActiveTask(Long userId, Long taskId) {
        return taskRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static void checkVersion(TaskEntity entity, Integer expectedVersion) {
        if (!Objects.equals(entity.getVersion(), expectedVersion)) {
            throw new AppException(ErrorCode.STALE_VERSION);
        }
    }

    private static TaskResponse toResponse(TaskEntity entity, Instant now) {
        boolean overdue = entity.getDeadline() != null
                && entity.getDeadline().isBefore(now)
                && entity.getStatus() != TaskStatus.COMPLETED
                && entity.getStatus() != TaskStatus.CANCELLED;

        return new TaskResponse(
                entity.getId(),
                entity.getGroupId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDeadline(),
                entity.getPriority(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getEstimatedDurationMinutes(),
                entity.getActualDurationMinutes(),
                overdue,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}