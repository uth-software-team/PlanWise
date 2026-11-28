package group16.webapp.modules.assignment.service;

import group16.webapp.modules.assignment.api.AssignmentStatus;
import group16.webapp.modules.assignment.dto.AssignmentCreateRequest;
import group16.webapp.modules.assignment.dto.AssignmentResponse;

import java.util.List;

/** UC-03 task assignment (AS-01, AS-02). Rights are checked on every call (D16), never from stored data. */
public interface AssignmentService {

    List<AssignmentResponse> list(Long callerId, AssignmentStatus status, Long groupId);

    AssignmentResponse create(Long callerId, AssignmentCreateRequest request);

    AssignmentResponse accept(Long callerId, Long assignmentId);

    AssignmentResponse decline(Long callerId, Long assignmentId, String reason);

    AssignmentResponse revoke(Long callerId, Long assignmentId);
}
