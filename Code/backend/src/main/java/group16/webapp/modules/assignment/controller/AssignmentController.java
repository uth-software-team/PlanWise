package group16.webapp.modules.assignment.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.assignment.api.AssignmentStatus;
import group16.webapp.modules.assignment.dto.AssignmentCreateRequest;
import group16.webapp.modules.assignment.dto.AssignmentResponse;
import group16.webapp.modules.assignment.dto.DeclineRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UC-03 task assignment. Contract stubs (GR-01): every method answers 501 NOT_IMPLEMENTED until AS-01
 * (create) and AS-02 (list, accept, decline, revoke) implement them. Planned errors: an authorized but stale
 * transition is 409 DATA_CONFLICT (conditional update); an existing PENDING/ACCEPTED assignment for the
 * same task is 409 ASSIGNMENT_ALREADY_ACTIVE.
 */
@RestController
@RequestMapping("/v1/assignments")
public class AssignmentController {

    /**
     * Without groupId: the caller's inbox (assignments where the caller is the assignee). With groupId: the
     * caller must be an ACTIVE LEADER of that group and gets its assignments. status is an optional filter.
     */
    @GetMapping
    public ApiResponse<List<AssignmentResponse>> listAssignments(
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) Long groupId) {
        throw notImplemented();
    }

    /**
     * ACTIVE LEADER of the task's group. For reassignment by taskId, personal, deleted and missing tasks give
     * the same 404; the assignee must be an ACTIVE member of the group.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentResponse> createAssignment(@Valid @RequestBody AssignmentCreateRequest request) {
        throw notImplemented();
    }

    /** Assignee only; PENDING -> ACCEPTED, sets respondedAt. */
    @PostMapping("/{id}/accept")
    public ApiResponse<AssignmentResponse> acceptAssignment(@PathVariable Long id) {
        throw notImplemented();
    }

    /** Assignee only; PENDING -> DECLINED with a reason, sets respondedAt. */
    @PostMapping("/{id}/decline")
    public ApiResponse<AssignmentResponse> declineAssignment(@PathVariable Long id,
                                                            @Valid @RequestBody DeclineRequest request) {
        throw notImplemented();
    }

    /** ACTIVE LEADER of the task's group; PENDING/ACCEPTED -> REVOKED, respondedAt is kept as it was. */
    @PostMapping("/{id}/revoke")
    public ApiResponse<AssignmentResponse> revokeAssignment(@PathVariable Long id) {
        throw notImplemented();
    }

    private static AppException notImplemented() {
        return new AppException(ErrorCode.NOT_IMPLEMENTED);
    }
}
