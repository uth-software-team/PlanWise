package group16.webapp.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * PUT /v1/users/me (design doc "ProfileUpdateRequest"). Only these fields can change; email, id and
 * password in the body are ignored. planningStart/planningEnd are optional but must be sent together
 * (display window "HH:mm[:ss]", BR-07).
 */
public record ProfileUpdateRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Size(max = 50) String timezone,
        LocalTime planningStart,
        LocalTime planningEnd
) {
}
