package group16.webapp.modules.task.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.dto.PageResponse;
import group16.webapp.common.util.SecurityUtils;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.dto.TaskCreateRequest;
import group16.webapp.modules.task.dto.TaskResponse;
import group16.webapp.modules.task.dto.TaskStatusRequest;
import group16.webapp.modules.task.dto.TaskUpdateRequest;
import group16.webapp.modules.task.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * UC-06 personal tasks (TK-02 CRUD, TK-03 status/filter/paging).
 * Owner-only (another user's task is 404), soft delete, optimistic locking with {@code version} (409 STALE_VERSION).
 */
@RestController
@RequestMapping("/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** The caller's non-deleted tasks, optionally filtered; {@code deadlineFrom}/{@code deadlineTo} are half-open. */
    @GetMapping
    public ApiResponse<PageResponse<TaskResponse>> listTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Instant deadlineFrom,
            @RequestParam(required = false) Instant deadlineTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.listTasks(userId, status, priority, deadlineFrom, deadlineTo, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.createTask(userId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> getTask(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.getTask(userId, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.updateTask(userId, id, request));
    }

    /** Soft delete (BR-03). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        taskService.deleteTask(userId, id);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TaskResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(taskService.changeStatus(userId, id, request));
    }
}