package group16.webapp.modules.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update a group (design doc GroupRequest). A missing description is stored as an empty string. */
public record GroupRequest(
        @NotBlank @Size(max = 100) String groupName,
        String description
) {
}
