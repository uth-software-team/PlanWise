package group16.webapp.modules.group.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.SecurityUtils;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.GroupResponse;
import group16.webapp.modules.group.dto.GroupSummaryResponse;
import group16.webapp.modules.group.dto.MemberAddRequest;
import group16.webapp.modules.group.dto.MemberResponse;
import group16.webapp.modules.group.dto.RoleUpdateRequest;
import group16.webapp.modules.group.service.GroupService;
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
 * UC-09 groups and members, plus the CG-11 availability endpoint. Group CRUD (GR-02) and member management (GR-03)
 * are implemented; availability (GR-04) is still a contract stub that answers 501 NOT_IMPLEMENTED.
 * Errors: 404 missing group or target, 403 insufficient membership or role, 409 DATA_CONFLICT for a duplicate
 * member or when the last leader would leave or be demoted.
 */
@RestController
@RequestMapping("/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
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

    /** ACTIVE LEADER only. */
    @PatchMapping("/{id}/members/{userId}")
    public ApiResponse<MemberResponse> changeMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                                        @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.ok(
                groupService.changeMemberRole(SecurityUtils.requireCurrentUserId(), id, userId, request));
    }

    /** ACTIVE LEADER removes a member, or a member leaves (userId = caller). Membership becomes INACTIVE. */
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(SecurityUtils.requireCurrentUserId(), id, userId);
    }

    /** CG-11: ACTIVE members see member ids and BUSY intervals only. */
    @GetMapping("/{id}/availability")
    public ApiResponse<GroupAvailabilityResponse> getAvailability(@PathVariable Long id,
                                                                  @RequestParam Instant start,
                                                                  @RequestParam Instant end) {
        throw notImplemented();
    }

    private static AppException notImplemented() {
        return new AppException(ErrorCode.NOT_IMPLEMENTED);
    }
}
