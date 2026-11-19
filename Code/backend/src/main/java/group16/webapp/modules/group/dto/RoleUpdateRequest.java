package group16.webapp.modules.group.dto;

import group16.webapp.modules.group.api.GroupRole;
import jakarta.validation.constraints.NotNull;

/** Leader changes a member's role (design doc RoleRequest). */
public record RoleUpdateRequest(
        @NotNull GroupRole role
) {
}
