package group16.webapp.modules.schedule;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.util.stream.Stream;

/**
 * SC-01 contract: scheduling stubs answer 501 for valid authenticated calls, validate input first (400
 * INVALID_INPUT vs 400 INVALID_TIME_RANGE) and are documented in OpenAPI.
 */
class ScheduleControllerStubTests extends AuthenticatedMvcTest {

	private static final String BATCH_SCHEMA = "#/components/schemas/ApiResponseProposalBatchResponse";

	private static final String GENERATE =
			"{\"windowStart\":\"2026-11-23T00:00:00Z\",\"windowEnd\":\"2026-11-30T00:00:00Z\"}";
	private static final String GENERATE_WITH_TASKS =
			"{\"windowStart\":\"2026-11-23T00:00:00Z\",\"windowEnd\":\"2026-11-30T00:00:00Z\",\"includeTaskIds\":[1,2]}";
	private static final String EDIT =
			"{\"startAt\":\"2026-11-23T09:00:00Z\",\"endAt\":\"2026-11-23T10:00:00Z\",\"version\":0}";

	static Stream<Arguments> endpoints() {
		return Stream.of(
				Arguments.of(HttpMethod.POST, "/v1/schedules/generate", GENERATE),
				Arguments.of(HttpMethod.POST, "/v1/schedules/generate", GENERATE_WITH_TASKS),
				Arguments.of(HttpMethod.GET, "/v1/schedules/proposals/1", null),
				Arguments.of(HttpMethod.DELETE, "/v1/schedules/proposals/1?version=0", null),
				Arguments.of(HttpMethod.PUT, "/v1/schedules/sessions/1", EDIT),
				Arguments.of(HttpMethod.DELETE, "/v1/schedules/sessions/1?version=3", null),
				Arguments.of(HttpMethod.POST, "/v1/schedules/1/confirm", "{\"version\":2}"));
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

	/** Present and well-formed, but empty (equal) or reversed. */
	static Stream<Arguments> invalidTimeRanges() {
		return Stream.of(
				Arguments.of("generate equal", HttpMethod.POST, "/v1/schedules/generate",
						"{\"windowStart\":\"2026-11-23T00:00:00Z\",\"windowEnd\":\"2026-11-23T00:00:00Z\"}"),
				Arguments.of("generate reversed", HttpMethod.POST, "/v1/schedules/generate",
						"{\"windowStart\":\"2026-11-30T00:00:00Z\",\"windowEnd\":\"2026-11-23T00:00:00Z\"}"),
				Arguments.of("edit equal", HttpMethod.PUT, "/v1/schedules/sessions/1",
						"{\"startAt\":\"2026-11-23T09:00:00Z\",\"endAt\":\"2026-11-23T09:00:00Z\",\"version\":0}"),
				Arguments.of("edit reversed", HttpMethod.PUT, "/v1/schedules/sessions/1",
						"{\"startAt\":\"2026-11-23T10:00:00Z\",\"endAt\":\"2026-11-23T09:00:00Z\",\"version\":0}"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidTimeRanges")
	void emptyOrReversedRangeReturnsInvalidTimeRange(String caseName, HttpMethod method, String uri, String body)
			throws Exception {
		callAuthenticated(method, uri, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
	}

	/** Missing or malformed input. */
	static Stream<Arguments> invalidInputs() {
		return Stream.of(
				Arguments.of("generate missing windowEnd", HttpMethod.POST, "/v1/schedules/generate",
						"{\"windowStart\":\"2026-11-23T00:00:00Z\"}"),
				Arguments.of("generate malformed windowStart", HttpMethod.POST, "/v1/schedules/generate",
						"{\"windowStart\":\"yesterday\",\"windowEnd\":\"2026-11-30T00:00:00Z\"}"),
				Arguments.of("generate null task id", HttpMethod.POST, "/v1/schedules/generate",
						"{\"windowStart\":\"2026-11-23T00:00:00Z\",\"windowEnd\":\"2026-11-30T00:00:00Z\","
								+ "\"includeTaskIds\":[null]}"),
				Arguments.of("generate without body", HttpMethod.POST, "/v1/schedules/generate", null),
				Arguments.of("edit missing version", HttpMethod.PUT, "/v1/schedules/sessions/1",
						"{\"startAt\":\"2026-11-23T09:00:00Z\",\"endAt\":\"2026-11-23T10:00:00Z\"}"),
				Arguments.of("edit missing startAt", HttpMethod.PUT, "/v1/schedules/sessions/1",
						"{\"endAt\":\"2026-11-23T10:00:00Z\",\"version\":0}"),
				Arguments.of("edit negative version", HttpMethod.PUT, "/v1/schedules/sessions/1",
						"{\"startAt\":\"2026-11-23T09:00:00Z\",\"endAt\":\"2026-11-23T10:00:00Z\",\"version\":-1}"),
				Arguments.of("edit non-numeric id", HttpMethod.PUT, "/v1/schedules/sessions/abc", EDIT),
				Arguments.of("delete session without version", HttpMethod.DELETE, "/v1/schedules/sessions/1", null),
				Arguments.of("delete session negative version", HttpMethod.DELETE,
						"/v1/schedules/sessions/1?version=-1", null),
				Arguments.of("delete proposal non-numeric id", HttpMethod.DELETE,
						"/v1/schedules/proposals/abc?version=0", null),
				Arguments.of("delete proposal without version", HttpMethod.DELETE, "/v1/schedules/proposals/1", null),
				Arguments.of("delete proposal non-numeric version", HttpMethod.DELETE,
						"/v1/schedules/proposals/1?version=x", null),
				Arguments.of("get proposal non-numeric id", HttpMethod.GET, "/v1/schedules/proposals/abc", null),
				Arguments.of("confirm empty body", HttpMethod.POST, "/v1/schedules/1/confirm", "{}"),
				Arguments.of("confirm negative version", HttpMethod.POST, "/v1/schedules/1/confirm",
						"{\"version\":-1}"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidInputs")
	void missingOrMalformedInputReturnsInvalidInput(String caseName, HttpMethod method, String uri, String body)
			throws Exception {
		callAuthenticated(method, uri, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void openApiDocumentsSuccessCodesAndSchemas() throws Exception {
		apiDocs()
				.andExpect(jsonPath("$.paths['/v1/schedules/generate'].post.responses['200'].content['*/*'].schema['$ref']")
						.value(BATCH_SCHEMA))
				.andExpect(jsonPath("$.paths['/v1/schedules/generate'].post.responses['201']").doesNotExist())
				.andExpect(jsonPath(
						"$.paths['/v1/schedules/proposals/{proposalId}'].get.responses['200'].content['*/*'].schema['$ref']")
						.value(BATCH_SCHEMA))
				.andExpect(jsonPath("$.paths['/v1/schedules/proposals/{proposalId}'].delete.responses['204']").exists())
				.andExpect(jsonPath("$.paths['/v1/schedules/proposals/{proposalId}'].delete.responses['204'].content")
						.doesNotExist())
				.andExpect(jsonPath("$.paths['/v1/schedules/proposals/{proposalId}'].delete.responses['200']")
						.doesNotExist())
				.andExpect(jsonPath(
						"$.paths['/v1/schedules/sessions/{id}'].put.responses['200'].content['*/*'].schema['$ref']")
						.value(BATCH_SCHEMA))
				.andExpect(jsonPath(
						"$.paths['/v1/schedules/sessions/{id}'].delete.responses['200'].content['*/*'].schema['$ref']")
						.value(BATCH_SCHEMA))
				.andExpect(jsonPath("$.paths['/v1/schedules/sessions/{id}'].delete.responses['204']").doesNotExist())
				.andExpect(jsonPath(
						"$.paths['/v1/schedules/{proposalId}/confirm'].post.responses['200'].content['*/*'].schema['$ref']")
						.value(BATCH_SCHEMA))
				.andExpect(jsonPath(
						"$.paths['/v1/schedules/proposals/{proposalId}'].delete.parameters[?(@.name=='version')].required")
						.value(contains(true)))
				.andExpect(jsonPath(
						"$.paths['/v1/schedules/sessions/{id}'].delete.parameters[?(@.name=='version')].required")
						.value(contains(true)));
	}

	@Test
	void openApiDocumentsRequestAndResponseSchemas() throws Exception {
		apiDocs()
				.andExpect(jsonPath("$.components.schemas.GenerateScheduleRequest.properties.userId").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.GenerateScheduleRequest.required")
						.value(containsInAnyOrder("windowStart", "windowEnd")))
				.andExpect(jsonPath("$.components.schemas.SessionEditRequest.properties.version").exists())
				.andExpect(jsonPath("$.components.schemas.ConfirmRequest.properties.version").exists())
				.andExpect(jsonPath("$.components.schemas.ProposedSession.properties.version").exists())
				.andExpect(jsonPath("$.components.schemas.ProposalBatchResponse.properties.version").exists())
				.andExpect(jsonPath("$.components.schemas.ProposalBatchResponse.properties.conflicts").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.ProposalBatchResponse.properties.status.enum")
						.value(contains("PROPOSED", "CONFIRMED", "CANCELLED")))
				.andExpect(jsonPath("$.components.schemas.ProposedSession.properties.status.enum")
						.value(contains("PROPOSED", "CONFIRMED", "COMPLETED", "CANCELLED")))
				.andExpect(jsonPath("$.components.schemas.UnscheduledTask.properties.reason.enum")
						.value(contains("DEADLINE_PASSED", "INSUFFICIENT_TIME")));
	}
}
