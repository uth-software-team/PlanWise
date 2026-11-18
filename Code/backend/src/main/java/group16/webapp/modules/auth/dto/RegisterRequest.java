package group16.webapp.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sign-up form (design doc UC-05: full name, email, password).
 * {@code timezone} is optional (IANA id, e.g. the browser's zone); the default is Asia/Ho_Chi_Minh.
 * Password: 8–72 characters (72 bytes is the BCrypt input limit, re-checked in UTF-8 by the service).
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 50) String timezone
) {
}
