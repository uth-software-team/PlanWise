package group16.webapp.modules.task;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.util.stream.Stream;

/** Contract tests for task endpoints: 401 authentication, 400 validation, and OpenAPI schemas. */
class TaskControllerStubTests extends AuthenticatedMvcTest {

    private static final String CREATE = "{\"title\":\"Write report\",\"description\":\"Chapter 3\","
            + "\"deadline\":\"2026-11-30T11:00:00Z\",\"priority\":\"HIGH\",\"category\":\"STUDY\","
            + "\"estimatedDurationMinutes\":90}";
    private static final String UPDATE = "{\"title\":\"Write report\",\"priority\":\"HIGH\",\"category\":\"STUDY\","
            + "\"estimatedDurationMinutes\":90,\"actualDurationMinutes\":30,\"version\":0}";
    private static final String STATUS = "{\"status\":\"IN_PROGRESS\",\"version\":0}";

    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/v1/tasks", null),
                Arguments.of(HttpMethod.GET, "/v1/tasks?status=TODO&priority=HIGH&deadlineFrom=2026-11-23T00:00:00Z"
                        + "&deadlineTo=2026-11-30T00:00:00Z&page=1&size=50", null),
                Arguments.of(HttpMethod.POST, "/v1/tasks", CREATE),
                Arguments.of(HttpMethod.POST, "/v1/tasks", "{\"title\":\"No deadline\",\"priority\":\"LOW\","
                        + "\"category\":\"OTHER\",\"estimatedDurationMinutes\":1}"),
                Arguments.of(HttpMethod.GET, "/v1/tasks/1", null),
                Arguments.of(HttpMethod.PUT, "/v1/tasks/1", UPDATE),
                Arguments.of(HttpMethod.DELETE, "/v1/tasks/1", null),
                Arguments.of(HttpMethod.PATCH, "/v1/tasks/1/status", STATUS));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void anonymousCallReturns401(HttpMethod method, String uri, String body) throws Exception {
        callAnonymous(method, uri, body)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("blank title", HttpMethod.POST, "/v1/tasks", CREATE.replace("Write report", "   ")),
                Arguments.of("tab/newline title", HttpMethod.POST, "/v1/tasks", CREATE.replace("Write report", "\\t\\n")),
                Arguments.of("missing title", HttpMethod.POST, "/v1/tasks", CREATE.replace("\"title\":\"Write report\",", "")),
                Arguments.of("title over 255", HttpMethod.POST, "/v1/tasks", CREATE.replace("Write report", "x".repeat(256))),
                Arguments.of("unknown priority", HttpMethod.POST, "/v1/tasks", CREATE.replace("\"HIGH\"", "\"CRITICAL\"")),
                Arguments.of("unknown category", HttpMethod.POST, "/v1/tasks", CREATE.replace("\"STUDY\"", "\"HOBBY\"")),
                Arguments.of("zero estimate", HttpMethod.POST, "/v1/tasks", CREATE.replace(":90", ":0")),
                Arguments.of("negative estimate", HttpMethod.POST, "/v1/tasks", CREATE.replace(":90", ":-5")),
                Arguments.of("missing estimate", HttpMethod.POST, "/v1/tasks",
                        CREATE.replace(",\"estimatedDurationMinutes\":90", "")),
                Arguments.of("malformed deadline", HttpMethod.POST, "/v1/tasks",
                        CREATE.replace("2026-11-30T11:00:00Z", "tomorrow")),
                Arguments.of("update without version", HttpMethod.PUT, "/v1/tasks/1", UPDATE.replace(",\"version\":0", "")),
                Arguments.of("update with negative version", HttpMethod.PUT, "/v1/tasks/1", UPDATE.replace(":0}", ":-1}")),
                Arguments.of("negative actual duration", HttpMethod.PUT, "/v1/tasks/1", UPDATE.replace(":30", ":-1")),
                Arguments.of("unknown status", HttpMethod.PATCH, "/v1/tasks/1/status", STATUS.replace("IN_PROGRESS", "DONE")),
                Arguments.of("status without version", HttpMethod.PATCH, "/v1/tasks/1/status", "{\"status\":\"TODO\"}"),
                Arguments.of("unknown status filter", HttpMethod.GET, "/v1/tasks?status=DONE", null),
                Arguments.of("negative page", HttpMethod.GET, "/v1/tasks?page=-1", null),
                Arguments.of("zero size", HttpMethod.GET, "/v1/tasks?size=0", null),
                Arguments.of("size over 100", HttpMethod.GET, "/v1/tasks?size=101", null),
                Arguments.of("malformed deadlineFrom", HttpMethod.GET, "/v1/tasks?deadlineFrom=yesterday", null),
                Arguments.of("non-numeric id", HttpMethod.GET, "/v1/tasks/abc", null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void invalidRequestReturns400(String caseName, HttpMethod method, String uri, String body) throws Exception {
        callAuthenticated(method, uri, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void openApiDocumentsMethodsSuccessCodesAndSchemas() throws Exception {
        apiDocs()
                .andExpect(jsonPath("$.paths['/v1/tasks'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponsePageResponseTaskResponse"))
                .andExpect(jsonPath("$.paths['/v1/tasks'].post.responses['201'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseTaskResponse"))
                .andExpect(jsonPath("$.paths['/v1/tasks'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/tasks/{id}'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseTaskResponse"))
                .andExpect(jsonPath("$.paths['/v1/tasks/{id}'].put.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseTaskResponse"))
                .andExpect(jsonPath("$.paths['/v1/tasks/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/v1/tasks/{id}'].delete.responses['204'].content").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/tasks/{id}/status'].patch.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseTaskResponse"))
                .andExpect(jsonPath("$.paths['/v1/tasks'].get.parameters[*].name")
                        .value(contains("status", "priority", "deadlineFrom", "deadlineTo", "page", "size")));
    }

    @Test
    void openApiDocumentsStableTaskEnums() throws Exception {
        apiDocs()
                .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.priority.enum")
                        .value(contains("LOW", "MEDIUM", "HIGH", "URGENT")))
                .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.category.enum")
                        .value(contains("STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER")))
                .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.status.enum")
                        .value(contains("TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED")))
                .andExpect(jsonPath("$.components.schemas.TaskCreateRequest.properties.priority.enum")
                        .value(contains("LOW", "MEDIUM", "HIGH", "URGENT")))
                .andExpect(jsonPath("$.components.schemas.TaskCreateRequest.properties.category.enum")
                        .value(contains("STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER")))
                .andExpect(jsonPath("$.components.schemas.TaskStatusRequest.properties.status.enum")
                        .value(contains("TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED")))
                .andExpect(jsonPath("$.paths['/v1/tasks'].get.parameters[?(@.name=='status')].schema.enum[*]")
                        .value(contains("TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED")));
    }
}