package group16.webapp.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;

/** Free text to turn into a task draft (design doc ParseRequest). Only this text is sent to the AI provider. */
public record TaskParseRequest(
        @NotBlank String text
) {
}
