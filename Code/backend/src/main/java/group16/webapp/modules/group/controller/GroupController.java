package group16.webapp.modules.group.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.GroupResponse;
import group16.webapp.modules.group.dto.GroupSummaryResponse;
import group16.webapp.modules.group.dto.MemberAddRequest;
import group16.webapp.modules.group.dto.MemberResponse;
import group16.webapp.modules.group.dto.RoleUpdateRequest;
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
 * UC-09 groups and members, plus the CG-11 availability endpoint. Contract stubs (GR-01): every method answers
 * 501 NOT_IMPLEMENTED until GR-02 (groups), GR-03 (members) and GR-04 (availability) implement them.
 * Planned errors: 404 missing group, 403 insufficient membership or role, 409 DATA_CONFLICT when the last
 * leader tries to leave.
 */
@RestController
@RequestMapping("/v1/groups")
public class GroupController {

    /** Groups the caller is an ACTIVE member of. */
    @GetMapping
    public ApiResponse<List<GroupSummaryResponse>> listMyGroups() {
        throw notImplemented();
    }

    /** Any user; the creator becomes an ACTIVE LEADER. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        throw notImplemented();
    }

    /** ACTIVE members only. */
    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> getGroup(@PathVariable Long id) {
        throw notImplemented();
    }

    /** ACTIVE LEADER only. */
    @PutMapping("/{id}")
    public ApiResponse<GroupResponse> updateGroup(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
        throw notImplemented();
    }

    /** ACTIVE LEADER only; adds (or reactivates) the user as MEMBER. */
    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> addMember(@PathVariable Long id, @Valid @RequestBody MemberAddRequest request) {
        throw notImplemented();
    }

    /** ACTIVE LEADER only. */
    @PatchMapping("/{id}/members/{userId}")
    public ApiResponse<MemberResponse> changeMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                                        @Valid @RequestBody RoleUpdateRequest request) {
        throw notImplemented();
    }

    /** ACTIVE LEADER removes a member, or a member leaves (userId = caller). Membership becomes INACTIVE. */
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        throw notImplemented();
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
