package group16.webapp.modules.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Leader adds an existing user by email (G8). Every admission assigns the MEMBER role; a former member's
 * row is reactivated and an old LEADER role is not restored.
 */
public record MemberAddRequest(
        @NotBlank @Email @Size(max = 255) String email
) {
}
