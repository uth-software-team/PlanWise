package group16.webapp.modules.auth.api;

import java.util.Optional;

/** Public contract of the auth module for other modules (e.g. group: add member by email; schedule: timezone). */
public interface UserApi {

    Optional<UserSummary> findById(Long userId);

    /** Email is normalized (trimmed, lower-cased) before the lookup. */
    Optional<UserSummary> findByEmail(String email);

    /** Normalization used for every stored and looked-up email. */
    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
