package group16.webapp.common.exception;

import group16.webapp.common.dto.ConflictErrorDto;
import group16.webapp.common.dto.ConflictErrorResponse;

import java.util.List;

/** The single exception type that business code should throw. */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    /** Optional response payload (e.g. conflicts[]); never put internal details here. */
    private final transient Object data;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public AppException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    /** 409 CONSTRAINT_VIOLATION with data = { conflicts: [...] } (design doc: BR-11, HC-01..09). */
    public static AppException conflicts(List<ConflictErrorDto> conflicts) {
        return new AppException(ErrorCode.CONSTRAINT_VIOLATION, ErrorCode.CONSTRAINT_VIOLATION.getMessage(),
                new ConflictErrorResponse(conflicts));
    }

    public ErrorCode getErrorCode() { return errorCode; }

    public Object getData() { return data; }
}
