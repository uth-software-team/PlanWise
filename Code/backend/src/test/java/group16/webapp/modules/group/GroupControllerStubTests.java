package group16.webapp.modules.group;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
 * GR-01 contract: every group endpoint needs a token, validates its input and is documented in OpenAPI. Group CRUD
 * and member behavior (GR-02, GR-03) are covered by GroupControllerTests, availability (GR-04) by
 * GroupAvailabilityControllerTests and GroupAvailabilityServiceImplTest.
 */
class GroupControllerStubTests extends AuthenticatedMvcTest {

	private static final String GROUP = "{\"groupName\":\"Team A\",\"description\":\"Course project\"}";

	/** Group CRUD endpoints implemented in GR-02 (no longer 501). */
	static Stream<Arguments> crudEndpoints() {
		return Stream.of(
				Arguments.of(HttpMethod.GET, "/v1/groups", null),
				Arguments.of(HttpMethod.POST, "/v1/groups", GROUP),
				Arguments.of(HttpMethod.GET, "/v1/groups/1", null),
				Arguments.of(HttpMethod.PUT, "/v1/groups/1", GROUP));
	}

	/** Member endpoints implemented in GR-03 (behavior covered by GroupControllerTests). */
	static Stream<Arguments> memberEndpoints() {
		return Stream.of(
				Arguments.of(HttpMethod.POST, "/v1/groups/1/members", "{\"email\":\"member@example.com\"}"),
				Arguments.of(HttpMethod.PATCH, "/v1/groups/1/members/2", "{\"role\":\"LEADER\"}"),
				Arguments.of(HttpMethod.DELETE, "/v1/groups/1/members/2", null));
	}

	/** Availability endpoint implemented in GR-04 (behavior covered by GroupAvailabilityControllerTests). */
	static Stream<Arguments> availabilityEndpoints() {
		return Stream.of(
				Arguments.of(HttpMethod.GET,
						"/v1/groups/1/availability?start=2026-11-23T00:00:00Z&end=2026-11-30T00:00:00Z", null));
	}

	static Stream<Arguments> allEndpoints() {
		return Stream.of(crudEndpoints(), memberEndpoints(), availabilityEndpoints()).flatMap(s -> s);
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("allEndpoints")
	void anonymousCallReturns401(HttpMethod method, String uri, String body) throws Exception {
		callAnonymous(method, uri, body)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	static Stream<Arguments> invalidRequests() {
		return Stream.of(
				Arguments.of(HttpMethod.POST, "/v1/groups", "{\"groupName\":\"  \"}"),
				Arguments.of(HttpMethod.POST, "/v1/groups", "{\"groupName\":\"%s\"}".formatted("x".repeat(101))),
				Arguments.of(HttpMethod.PUT, "/v1/groups/1", "{}"),
				Arguments.of(HttpMethod.GET, "/v1/groups/abc", null),
				Arguments.of(HttpMethod.POST, "/v1/groups/1/members", "{\"email\":\"not-an-email\"}"),
				Arguments.of(HttpMethod.PATCH, "/v1/groups/1/members/2", "{\"role\":\"OWNER\"}"),
				Arguments.of(HttpMethod.PATCH, "/v1/groups/1/members/2", "{}"),
				Arguments.of(HttpMethod.GET, "/v1/groups/1/availability?start=2026-11-23T00:00:00Z", null),
				Arguments.of(HttpMethod.GET, "/v1/groups/1/availability?start=yesterday&end=2026-11-30T00:00:00Z",
						null));
	}

	@ParameterizedTest(name = "{0} {1} {2}")
	@MethodSource("invalidRequests")
	void invalidRequestReturns400(HttpMethod method, String uri, String body) throws Exception {
		callAuthenticated(method, uri, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void openApiDocumentsMethodsSuccessCodesAndSchemas() throws Exception {
		apiDocs()
				.andExpect(jsonPath("$.paths['/v1/groups'].get.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseListGroupSummaryResponse"))
				.andExpect(jsonPath("$.paths['/v1/groups'].post.responses['201'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseGroupResponse"))
				.andExpect(jsonPath("$.paths['/v1/groups'].post.responses['200']").doesNotExist())
				.andExpect(jsonPath("$.paths['/v1/groups/{id}'].get.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseGroupResponse"))
				.andExpect(jsonPath("$.paths['/v1/groups/{id}'].put.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseGroupResponse"))
				.andExpect(jsonPath(
						"$.paths['/v1/groups/{id}/members'].post.responses['201'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseMemberResponse"))
				.andExpect(jsonPath(
						"$.paths['/v1/groups/{id}/members/{userId}'].patch.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseMemberResponse"))
				.andExpect(jsonPath("$.paths['/v1/groups/{id}/members/{userId}'].delete.responses['204']").exists())
				.andExpect(jsonPath("$.paths['/v1/groups/{id}/members/{userId}'].delete.responses['204'].content")
						.doesNotExist())
				.andExpect(jsonPath(
						"$.paths['/v1/groups/{id}/availability'].get.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseGroupAvailabilityResponse"))
				.andExpect(jsonPath("$.paths['/v1/groups/{id}/availability'].get.parameters[?(@.name=='start')].required")
						.value(contains(true)))
				.andExpect(jsonPath("$.paths['/v1/groups/{id}/availability'].get.parameters[?(@.name=='end')].required")
						.value(contains(true)));
	}

	@Test
	void openApiDocumentsGroupEnumsAndBusyOnlyAvailability() throws Exception {
		apiDocs()
				.andExpect(jsonPath("$.components.schemas.MemberResponse.properties.role.enum")
						.value(contains("LEADER", "MEMBER")))
				.andExpect(jsonPath("$.components.schemas.MemberResponse.properties.status.enum")
						.value(contains("ACTIVE", "INACTIVE")))
				.andExpect(jsonPath("$.components.schemas.RoleUpdateRequest.properties.role.enum")
						.value(contains("LEADER", "MEMBER")))
				.andExpect(jsonPath("$.components.schemas.GroupSummaryResponse.properties.myRole.enum")
						.value(contains("LEADER", "MEMBER")))
				.andExpect(jsonPath("$.components.schemas.MemberBusy.properties.*").value(hasSize(2)))
				.andExpect(jsonPath("$.components.schemas.MemberBusy.properties.userId").exists())
				.andExpect(jsonPath("$.components.schemas.MemberBusy.properties.busy").exists())
				.andExpect(jsonPath("$.components.schemas.BusyInterval.properties.*").value(hasSize(2)));
	}
}
