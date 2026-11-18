package group16.webapp.common.dto;

import java.util.List;

/** {@code data} of a 409 CONSTRAINT_VIOLATION response: every violated rule, per session. */
public record ConflictErrorResponse(List<ConflictErrorDto> conflicts) {

    public ConflictErrorResponse {
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    }
}
