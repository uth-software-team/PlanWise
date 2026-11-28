package group16.webapp.modules.group.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.util.SecurityUtils;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.GroupResponse;
import group16.webapp.modules.group.dto.GroupSummaryResponse;
import group16.webapp.modules.group.dto.LeaderTransferRequest;
import group16.webapp.modules.group.dto.MemberAddRequest;
import group16.webapp.modules.group.dto.MemberResponse;
import group16.webapp.modules.group.dto.RoleUpdateRequest;
import group16.webapp.modules.group.service.GroupAvailabilityService;
import group16.webapp.modules.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
import java.util.List;

/**
 * UC-09 groups and members, plus the CG-11 availability endpoint. Group CRUD (GR-02), member management (GR-03) and
 * availability (GR-04) are implemented. A group with ACTIVE members has exactly one ACTIVE LEADER (D16). Errors: 404
 * missing group or target, 403 insufficient membership or role, 409 DATA_CONFLICT for a duplicate member, a role
 * change or a LEADER leaving without a successor, 409 STALE_VERSION when a leave request no longer matches the
 * caller's role or successor; availability adds 400 INVALID_TIME_RANGE (empty, reversed or longer than 366 days)
 * and 501 NOT_IMPLEMENTED while no calendar provider (CAL-01) exists.
 */
@RestController
@RequestMapping("/v1/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupAvailabilityService availabilityService;

    public GroupController(GroupService groupService, GroupAvailabilityService availabilityService) {
        this.groupService = groupService;
        this.availabilityService = availabilityService;
    }

    /** Groups the caller is an ACTIVE member of. */
    @GetMapping
    public ApiResponse<List<GroupSummaryResponse>> listMyGroups() {
        return ApiResponse.ok(groupService.listMyGroups(SecurityUtils.requireCurrentUserId()));
    }

    /** Any user; the creator becomes an ACTIVE LEADER. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        return ApiResponse.ok(groupService.createGroup(SecurityUtils.requireCurrentUserId(), request));
    }

    /** ACTIVE members only. */
    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> getGroup(@PathVariable Long id) {
        return ApiResponse.ok(groupService.getGroup(SecurityUtils.requireCurrentUserId(), id));
    }

    /** ACTIVE LEADER only. */
    @PutMapping("/{id}")
    public ApiResponse<GroupResponse> updateGroup(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        return ApiResponse.ok(groupService.updateGroup(SecurityUtils.requireCurrentUserId(), id, request));
    }

    /** ACTIVE LEADER only; adds (or reactivates) the user as MEMBER. */
    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> addMember(@PathVariable Long id, @Valid @RequestBody MemberAddRequest request) {
        return ApiResponse.ok(groupService.addMember(SecurityUtils.requireCurrentUserId(), id, request));
    }

    /** ACTIVE LEADER only. Roles cannot change here (D16); use transfer-leadership. */
    @Operation(description = "ACTIVE LEADER only. Roles cannot be changed with this endpoint (D16): the target's "
            + "current role returns 200 and changes nothing; any other role is 409 DATA_CONFLICT. Use "
            + "POST /v1/groups/{id}/transfer-leadership to change the LEADER. 404 when the target is not an ACTIVE "
            + "member.")
    @PatchMapping("/{id}/members/{userId}")
    public ApiResponse<MemberResponse> changeMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                                        @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.ok(
                groupService.changeMemberRole(SecurityUtils.requireCurrentUserId(), id, userId, request));
    }

    /** ACTIVE LEADER only: the caller becomes MEMBER and the target LEADER. */
    @Operation(description = "ACTIVE LEADER only. The caller becomes MEMBER and newLeaderUserId becomes LEADER in "
            + "one transaction. 409 DATA_CONFLICT when newLeaderUserId is the caller; 404 when it is not an ACTIVE "
            + "member or the account is inactive. Returns the updated group.")
    @PostMapping("/{id}/transfer-leadership")
    public ApiResponse<GroupResponse> transferLeadership(@PathVariable Long id,
                                                         @Valid @RequestBody LeaderTransferRequest request) {
        return ApiResponse.ok(groupService.transferLeadership(SecurityUtils.requireCurrentUserId(), id, request));
    }

    /** The ACTIVE LEADER removes a MEMBER, or a member leaves (userId = caller). Membership becomes INACTIVE. */
    @Operation(description = "Removal (userId is not the caller): ACTIVE LEADER only; expectedSuccessorUserId must "
            + "be absent (400 INVALID_INPUT); the LEADER cannot be removed (409 DATA_CONFLICT). Self-leave "
            + "(userId = caller): a MEMBER sends no expectedSuccessorUserId; the LEADER must send the group's "
            + "successorUserId and that member becomes LEADER; 409 DATA_CONFLICT when the group has no successor. "
            + "409 STALE_VERSION, with no change, when the caller's role or the successor no longer matches.")
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId,
                             @RequestParam(required = false) Long expectedSuccessorUserId) {
        groupService.removeMember(SecurityUtils.requireCurrentUserId(), id, userId, expectedSuccessorUserId);
    }

    /** CG-11: ACTIVE members see member ids and BUSY intervals only, within the half-open window [start, end). */
    @Operation(description = "Half-open window [start, end). Requires start < end and end - start <= 366 days; "
            + "otherwise 400 INVALID_TIME_RANGE.")
    @GetMapping("/{id}/availability")
    public ApiResponse<GroupAvailabilityResponse> getAvailability(@PathVariable Long id,
                                                                  @RequestParam Instant start,
                                                                  @RequestParam Instant end) {
        return ApiResponse.ok(
                availabilityService.getAvailability(SecurityUtils.requireCurrentUserId(), id, start, end));
    }
}
