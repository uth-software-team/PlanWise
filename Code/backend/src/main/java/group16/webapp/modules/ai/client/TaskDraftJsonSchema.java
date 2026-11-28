package group16.webapp.modules.ai.client;

import group16.webapp.modules.task.api.TaskCategory;
import group16.webapp.modules.task.api.TaskPriority;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON schema sent to Gemini as {@code generationConfig.responseJsonSchema} for task parsing. Mirrors the five
 * optional fields of {@link group16.webapp.modules.ai.dto.TaskDraftResponse} (no {@code required}). Two
 * constraints exist only in this provider schema: {@code minimum: 1} (V4 {@code ck_tasks_estimated_duration_positive})
 * and {@code additionalProperties: false}. The provider may ignore schema keywords, so AI-02 still validates
 * every field.
 */
public final class TaskDraftJsonSchema {

    public static final Map<String, Object> SCHEMA = build();

    private TaskDraftJsonSchema() {
    }

    private static Map<String, Object> build() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("title", Map.of("type", "string"));
        properties.put("deadline", Map.of("type", "string", "format", "date-time"));
        properties.put("priority", Map.of("type", "string", "enum", names(TaskPriority.values())));
        properties.put("category", Map.of("type", "string", "enum", names(TaskCategory.values())));
        properties.put("estimatedDurationMinutes", Map.of("type", "integer", "minimum", 1));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections.unmodifiableMap(properties));
        schema.put("additionalProperties", false);
        return Collections.unmodifiableMap(schema);
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
