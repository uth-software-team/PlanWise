package group16.webapp.modules.availability.config;

import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Makes the generated schema of {@code FixedEventRequest} express which fields each recurrence type requires
 * (enforced at runtime by {@code FixedEventRequest#isRequiredRecurrenceFieldsPresent}, which stays hidden).
 */
@Configuration
public class FixedEventOpenApiConfig {

    static final String REQUEST_SCHEMA = "FixedEventRequest";

    @Bean
    OpenApiCustomizer fixedEventRequestRecurrenceFields() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                return;
            }
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            Schema<?> schema = schemas == null ? null : schemas.get(REQUEST_SCHEMA);
            if (schema == null) {
                return;
            }
            schema.setRequired(new ArrayList<>(List.of("title", "recurrenceType")));
            schema.setOneOf(new ArrayList<>(List.of(
                    branch("ONCE", "startAt", "endAt"),
                    branch("WEEKLY", "dayOfWeek", "startTime", "endTime"))));
            schema.setDescription("ONCE requires startAt and endAt; WEEKLY requires dayOfWeek, startTime and endTime.");
        };
    }

    /** Branch: recurrenceType equals {@code type} and the listed fields are required. */
    private static Schema<Object> branch(String type, String... requiredFields) {
        Schema<Object> typeValue = new Schema<>();
        typeValue.setEnum(new ArrayList<>(List.of(type)));
        List<String> required = new ArrayList<>(List.of("recurrenceType"));
        required.addAll(List.of(requiredFields));
        Schema<Object> branch = new Schema<>();
        // setRequired drops names missing from an existing "properties" map, so set it before adding the property.
        branch.setRequired(required);
        branch.addProperty("recurrenceType", typeValue);
        return branch;
    }
}
