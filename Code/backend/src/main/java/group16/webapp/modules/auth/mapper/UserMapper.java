package group16.webapp.modules.auth.mapper;

import group16.webapp.modules.auth.dto.UserResponse;
import group16.webapp.modules.auth.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getTimezone(),
                user.getPlanningStart(), user.getPlanningEnd(), user.getCreatedAt());
    }
}
