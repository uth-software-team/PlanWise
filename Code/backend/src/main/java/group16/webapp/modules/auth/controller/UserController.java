package group16.webapp.modules.auth.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.util.SecurityUtils;
import group16.webapp.modules.auth.dto.ProfileUpdateRequest;
import group16.webapp.modules.auth.dto.UserResponse;
import group16.webapp.modules.auth.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC-05 profile of the current user. The user id always comes from the JWT, never from the client. */
@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(profileService.getCurrent(SecurityUtils.requireCurrentUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMe(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok("Profile updated", profileService.update(SecurityUtils.requireCurrentUserId(), request));
    }
}
