package group16.webapp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Central catalogue of business/technical errors. Frontend can rely on the enum name.
 * Names follow the design document's REST contract (INVALID_INPUT, UNAUTHENTICATED, STALE_VERSION, ...).
 * Module-specific codes (INVALID_TIME_RANGE, NO_AVAILABILITY_RULE, CONSTRAINT_VIOLATION, AI_UNAVAILABLE, ...)
 * are added together with the module that throws them.
 */
public enum ErrorCode {
    // ---- Generic ----
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Invalid input"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this resource"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type"),
    STALE_VERSION(HttpStatus.CONFLICT, "Data was changed by another request. Please reload and try again"),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Request conflicts with existing data"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    // ---- Auth ----
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),

    // ---- User ----
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already in use");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
