package group16.webapp.modules.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** GR-01 contract: assignment stubs answer 501, validate their input and are documented in OpenAPI. */
class AssignmentControllerStubTests extends AuthenticatedMvcTest {

	private static final String NEW_TASK = "{\"groupId\":1,\"title\":\"Write report\",\"description\":\"\","
			+ "\"deadline\":\"2026-11-30T11:00:00Z\",\"priority\":\"HIGH\",\"category\":\"PROJECT\","
			+ "\"estimatedDurationMinutes\":90}";

	static Stream<Arguments> endpoints() {
		return Stream.of(
				Arguments.of(HttpMethod.GET, "/v1/assignments", null),
				Arguments.of(HttpMethod.GET, "/v1/assignments?status=PENDING&groupId=1", null),
				Arguments.of(HttpMethod.POST, "/v1/assignments", "{\"assigneeUserId\":2,\"newTask\":" + NEW_TASK + "}"),
				Arguments.of(HttpMethod.POST, "/v1/assignments", "{\"assigneeUserId\":2,\"taskId\":5}"),
				Arguments.of(HttpMethod.POST, "/v1/assignments/1/accept", null),
				Arguments.of(HttpMethod.POST, "/v1/assignments/1/decline", "{\"reason\":\"Busy this week\"}"),
				Arguments.of(HttpMethod.POST, "/v1/assignments/1/revoke", null));
	}

	@ParameterizedTest(name = "{0} {1} {2}")
	@MethodSource("endpoints")
	void authenticatedValidCallReturns501(HttpMethod method, String uri, String body) throws Exception {
		callAuthenticated(method, uri, body)
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
	}

	@ParameterizedTest(name = "{0} {1} {2}")
	@MethodSource("endpoints")
	void anonymousCallReturns401(HttpMethod method, String uri, String body) throws Exception {
		callAnonymous(method, uri, body)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	static Stream<Arguments> invalidRequests() {
		return Stream.of(
				Arguments.of("both newTask and taskId", HttpMethod.POST, "/v1/assignments",
						"{\"assigneeUserId\":2,\"taskId\":5,\"newTask\":" + NEW_TASK + "}"),
				Arguments.of("neither newTask nor taskId", HttpMethod.POST, "/v1/assignments",
						"{\"assigneeUserId\":2}"),
				Arguments.of("missing assignee", HttpMethod.POST, "/v1/assignments", "{\"taskId\":5}"),
				Arguments.of("unknown priority", HttpMethod.POST, "/v1/assignments",
						"{\"assigneeUserId\":2,\"newTask\":" + NEW_TASK.replace("\"HIGH\"", "\"CRITICAL\"") + "}"),
				Arguments.of("unknown category", HttpMethod.POST, "/v1/assignments",
						"{\"assigneeUserId\":2,\"newTask\":" + NEW_TASK.replace("\"PROJECT\"", "\"HOBBY\"") + "}"),
				Arguments.of("zero duration", HttpMethod.POST, "/v1/assignments",
						"{\"assigneeUserId\":2,\"newTask\":" + NEW_TASK.replace(":90", ":0") + "}"),
				Arguments.of("blank title", HttpMethod.POST, "/v1/assignments",
						"{\"assigneeUserId\":2,\"newTask\":" + NEW_TASK.replace("Write report", "  ") + "}"),
				Arguments.of("missing reason", HttpMethod.POST, "/v1/assignments/1/decline", "{}"),
				Arguments.of("null reason", HttpMethod.POST, "/v1/assignments/1/decline", "{\"reason\":null}"),
				Arguments.of("spaces reason", HttpMethod.POST, "/v1/assignments/1/decline", "{\"reason\":\"   \"}"),
				Arguments.of("tab/newline reason", HttpMethod.POST, "/v1/assignments/1/decline",
						"{\"reason\":\"\\t\\n\"}"),
				Arguments.of("unknown status filter", HttpMethod.GET, "/v1/assignments?status=DONE", null),
				Arguments.of("non-numeric groupId", HttpMethod.GET, "/v1/assignments?groupId=abc", null));
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
				.andExpect(jsonPath("$.paths['/v1/assignments'].get.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseListAssignmentResponse"))
				.andExpect(jsonPath("$.paths['/v1/assignments'].get.parameters[?(@.name=='status')].required")
						.value(contains(false)))
				.andExpect(jsonPath("$.paths['/v1/assignments'].get.parameters[?(@.name=='groupId')].required")
						.value(contains(false)))
				.andExpect(jsonPath("$.paths['/v1/assignments'].post.responses['201'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseAssignmentResponse"))
				.andExpect(jsonPath("$.paths['/v1/assignments'].post.responses['200']").doesNotExist())
				.andExpect(jsonPath(
						"$.paths['/v1/assignments/{id}/accept'].post.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseAssignmentResponse"))
				.andExpect(jsonPath(
						"$.paths['/v1/assignments/{id}/decline'].post.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseAssignmentResponse"))
				.andExpect(jsonPath(
						"$.paths['/v1/assignments/{id}/revoke'].post.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseAssignmentResponse"));
	}

	@Test
	void openApiDocumentsStableEnumValues() throws Exception {
		apiDocs()
				.andExpect(jsonPath("$.paths['/v1/assignments'].get.parameters[?(@.name=='status')].schema.enum[*]")
						.value(contains("PENDING", "ACCEPTED", "DECLINED", "REVOKED")))
				.andExpect(jsonPath("$.components.schemas.AssignmentResponse.properties.assignmentStatus.enum")
						.value(contains("PENDING", "ACCEPTED", "DECLINED", "REVOKED")))
				.andExpect(jsonPath("$.components.schemas.AssignmentTaskSummary.properties.priority.enum")
						.value(contains("LOW", "MEDIUM", "HIGH", "URGENT")))
				.andExpect(jsonPath("$.components.schemas.AssignmentTaskSummary.properties.taskStatus.enum")
						.value(contains("TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED")))
				.andExpect(jsonPath("$.components.schemas.NewGroupTask.properties.priority.enum")
						.value(contains("LOW", "MEDIUM", "HIGH", "URGENT")))
				.andExpect(jsonPath("$.components.schemas.NewGroupTask.properties.category.enum")
						.value(contains("STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER")))
				.andExpect(jsonPath("$.components.schemas.AssignmentCreateRequest.required")
						.value(hasItem("assigneeUserId")))
				.andExpect(jsonPath("$.components.schemas.AssignmentCreateRequest.properties.exactlyOneTaskSource")
						.doesNotExist())
				.andExpect(jsonPath("$.components.schemas.AssignmentTaskSummary.properties.*")
						.value(hasSize(5)))
				.andExpect(jsonPath("$.components.schemas.AssignmentResponse.properties.status").doesNotExist());
	}

	@Test
	void generatedCreateSchemaRequiresAssigneeAndExactlyOneTaskSource() throws Exception {
		String docs = apiDocs().andReturn().getResponse().getContentAsString();
		Map<String, Object> schema = JsonPath.read(docs, "$.components.schemas.AssignmentCreateRequest");
		Map<String, Object> newTask = Map.of("groupId", 1, "title", "Write report", "priority", "HIGH",
				"category", "PROJECT", "estimatedDurationMinutes", 90);

		assertThat(satisfies(schema, Map.of("assigneeUserId", 2, "newTask", newTask))).as("newTask only").isTrue();
		assertThat(satisfies(schema, Map.of("assigneeUserId", 2, "taskId", 5))).as("taskId only").isTrue();
		assertThat(satisfies(schema, Map.of("assigneeUserId", 2, "newTask", newTask, "taskId", 5)))
				.as("both newTask and taskId").isFalse();
		assertThat(satisfies(schema, Map.of("assigneeUserId", 2))).as("neither newTask nor taskId").isFalse();
		assertThat(satisfies(schema, Map.of("taskId", 5))).as("missing assigneeUserId").isFalse();
	}

	/**
	 * Applies the object-level keywords of the generated schema (required, not, oneOf) to a request body.
	 * Property formats are covered by the HTTP validation tests above. A oneOf branch or a "not" schema that
	 * uses any other keyword fails the test, so no part of the rule is silently ignored.
	 */
	@SuppressWarnings("unchecked")
	private static boolean satisfies(Map<String, Object> schema, Map<String, Object> body) {
		List<String> required = (List<String>) schema.getOrDefault("required", List.of());
		if (!body.keySet().containsAll(required)) {
			return false;
		}
		if (schema.containsKey("not")) {
			Map<String, Object> not = (Map<String, Object>) schema.get("not");
			assertThat(not.keySet()).isSubsetOf("required");
			if (satisfies(not, body)) {
				return false;
			}
		}
		if (schema.containsKey("oneOf")) {
			int matches = 0;
			for (Map<String, Object> branch : (List<Map<String, Object>>) schema.get("oneOf")) {
				assertThat(branch.keySet()).isSubsetOf("required", "not");
				if (satisfies(branch, body)) {
					matches++;
				}
			}
			return matches == 1;
		}
		return true;
	}
}
