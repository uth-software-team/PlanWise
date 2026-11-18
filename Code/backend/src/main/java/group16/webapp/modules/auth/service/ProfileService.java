package group16.webapp.modules.auth.service;

import group16.webapp.modules.auth.dto.ProfileUpdateRequest;
import group16.webapp.modules.auth.dto.UserResponse;

public interface ProfileService {

    /** 401 UNAUTHENTICATED when the token's user no longer exists or is inactive. */
    UserResponse getCurrent(Long userId);

    /** Updates name, timezone and (optionally) the planning window; returns the new profile. */
    UserResponse update(Long userId, ProfileUpdateRequest request);
}
