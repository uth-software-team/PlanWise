package group16.webapp.modules.assignment.config;

import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Makes the generated schema of {@code AssignmentCreateRequest} express the runtime rule that annotations cannot:
 * {@code assigneeUserId} plus exactly one of {@code newTask} or {@code taskId}
 * (enforced at runtime by {@code AssignmentCreateRequest#isExactlyOneTaskSource}, which stays hidden).
 */
@Configuration
public class AssignmentOpenApiConfig {

    static final String CREATE_REQUEST_SCHEMA = "AssignmentCreateRequest";

    @Bean
    OpenApiCustomizer assignmentCreateRequestExactlyOneTaskSource() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                return;
            }
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            Schema<?> schema = schemas == null ? null : schemas.get(CREATE_REQUEST_SCHEMA);
            if (schema == null) {
                return;
            }
            schema.setRequired(new ArrayList<>(List.of("assigneeUserId")));
            schema.setOneOf(new ArrayList<>(List.of(
                    presentWithout("newTask", "taskId"),
                    presentWithout("taskId", "newTask"))));
            schema.setDescription("assigneeUserId plus exactly one of newTask (first assignment) or taskId "
                    + "(reassignment of an existing group task). Omit the unused field.");
        };
    }

    /** Branch: {@code present} is required and {@code absent} must not be sent. */
    private static Schema<Object> presentWithout(String present, String absent) {
        Schema<Object> forbidden = new Schema<>();
        forbidden.setRequired(new ArrayList<>(List.of(absent)));
        Schema<Object> branch = new Schema<>();
        branch.setRequired(new ArrayList<>(List.of(present)));
        branch.setNot(forbidden);
        return branch;
    }
}
