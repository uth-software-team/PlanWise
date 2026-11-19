package group16.webapp.modules.assignment.dto;

import jakarta.validation.constraints.NotBlank;

/** A decline needs a reason with at least one non-whitespace character (BR-05; V8 ck_task_assignments_decline_reason). */
public record DeclineRequest(
        @NotBlank String reason
) {
}
