package group16.webapp.modules.auth.api;

/** Public, non-sensitive view of a user for other modules (never contains the password hash). */
public record UserSummary(Long id, String email, String fullName, String timezone, boolean active) {
}
