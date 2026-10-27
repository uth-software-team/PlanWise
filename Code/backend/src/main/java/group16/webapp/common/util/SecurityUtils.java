package group16.webapp.common.util;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/** Read information of the currently logged-in user from the JWT. */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<Long> getCurrentUserId() {
        return getJwtAuth().map(auth -> {
            try {
                return Long.valueOf(auth.getToken().getSubject());
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }

    /** Same as getCurrentUserId() but throws 401 when there is no logged-in user. */
    public static Long requireCurrentUserId() {
        return getCurrentUserId().orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }

    public static Optional<String> getCurrentUserEmail() {
        return getJwtAuth().map(auth -> auth.getToken().getClaimAsString("email"));
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private static Optional<JwtAuthenticationToken> getJwtAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwtAuth ? Optional.of(jwtAuth) : Optional.empty();
    }
}
