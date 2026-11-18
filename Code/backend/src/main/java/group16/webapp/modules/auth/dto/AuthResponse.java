package group16.webapp.modules.auth.dto;

import java.time.Instant;

/** Login result: send {@code accessToken} as "Authorization: Bearer ..." until {@code expiresAt}. */
public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {

    public static final String BEARER = "Bearer";
}
