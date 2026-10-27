package group16.webapp.common.exception;

import org.springframework.http.HttpStatus;

/** Central catalogue of business/technical errors. Frontend can rely on the enum name. */
public enum ErrorCode {
    // ---- Generic ----
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    // ---- Auth ----
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),

    // ---- User ----
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already in use"),

    // ---- Project ----
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Project not found"),

    // ---- Task ----
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task not found");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
