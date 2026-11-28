package group16.webapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * F-05 contract freeze guard: the generated OpenAPI document must expose exactly the MVP endpoints and
 * describe auth, statuses, server base and internal references the way the runtime behaves. Changing the
 * endpoint set needs a TV1-approved contract change (docs/plan/01_CONTRACT_GATES.md) and a new snapshot
 * ({@code ./gradlew exportOpenApi}).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiContractTests extends AuthenticatedMvcTest {

	/** The 41 MVP operations under /api (design doc table 4.3 minus P1 cuts and /join, plus CG-11 and D16 transfer). */
	static final Set<String> EXPECTED_OPERATIONS = Set.of(
			"POST /v1/auth/register", "POST /v1/auth/login", "POST /v1/auth/logout",
			"GET /v1/users/me", "PUT /v1/users/me",
			"GET /v1/tasks", "POST /v1/tasks", "GET /v1/tasks/{id}", "PUT /v1/tasks/{id}",
			"DELETE /v1/tasks/{id}", "PATCH /v1/tasks/{id}/status",
			"GET /v1/availability-rules", "POST /v1/availability-rules",
			"PUT /v1/availability-rules/{id}", "DELETE /v1/availability-rules/{id}",
			"GET /v1/fixed-events", "POST /v1/fixed-events",
			"PUT /v1/fixed-events/{id}", "DELETE /v1/fixed-events/{id}",
			"GET /v1/calendar",
			"GET /v1/groups", "POST /v1/groups", "GET /v1/groups/{id}", "PUT /v1/groups/{id}",
			"POST /v1/groups/{id}/members", "PATCH /v1/groups/{id}/members/{userId}",
			"DELETE /v1/groups/{id}/members/{userId}", "POST /v1/groups/{id}/transfer-leadership",
			"GET /v1/groups/{id}/availability",
			"GET /v1/assignments", "POST /v1/assignments", "POST /v1/assignments/{id}/accept",
			"POST /v1/assignments/{id}/decline", "POST /v1/assignments/{id}/revoke",
			"POST /v1/schedules/generate", "GET /v1/schedules/proposals/{proposalId}",
			"DELETE /v1/schedules/proposals/{proposalId}", "PUT /v1/schedules/sessions/{id}",
			"DELETE /v1/schedules/sessions/{id}", "POST /v1/schedules/{proposalId}/confirm",
			"POST /v1/ai/tasks/parse");

	private static final Set<String> HTTP_METHODS =
			Set.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

	private Map<String, Object> docs;

	@BeforeAll
	void loadDocs() throws Exception {
		String json = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.get("/v3/api-docs"))
				.andReturn().getResponse().getContentAsString();
		docs = parse(json);
	}

	@Test
	void exposesExactlyTheMvpOperations() {
		assertThat(EXPECTED_OPERATIONS).hasSize(41);
		assertThat(operations(docs)).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATIONS);
	}

	@Test
	void registerAndLoginArePublicAndLogoutNeedsBearer() {
		assertThat(operation("/v1/auth/register", "post")).containsEntry("security", List.of());
		assertThat(operation("/v1/auth/login", "post")).containsEntry("security", List.of());
		assertThat(effectiveSecurity("/v1/auth/logout", "post")).isEqualTo(List.of(Map.of("bearerAuth", List.of())));
		assertThat(effectiveSecurity("/v1/users/me", "get")).isEqualTo(List.of(Map.of("bearerAuth", List.of())));
	}

	@Test
	void registerDocuments201AndLogoutDocuments204WithoutContent() {
		Map<String, Object> register = responses("/v1/auth/register", "post");
		assertThat(register).containsKey("201").doesNotContainKey("200");

		Map<String, Object> logout = responses("/v1/auth/logout", "post");
		assertThat(logout).containsKey("204").doesNotContainKey("200");
		assertThat(asMap(logout.get("204"))).doesNotContainKey("content");
	}

	@Test
	void serverBaseIsApiContextPath() {
		assertThat(docs.get("servers")).isEqualTo(List.of(Map.of("url", "/api")));
	}

	@Test
	void everyInternalReferenceResolves() {
		assertThat(unresolvedRefs(docs)).isEmpty();
	}

	// ---------- shared with OpenApiSnapshotExportTests ----------

	@SuppressWarnings("unchecked")
	static Map<String, Object> parse(String json) {
		return JsonMapper.builder().build().readValue(json, Map.class);
	}

	/** "METHOD path" for every operation in the document. */
	static Set<String> operations(Map<String, Object> document) {
		Set<String> result = new TreeSet<>();
		asMap(document.get("paths")).forEach((path, item) -> asMap(item).keySet().stream()
				.filter(HTTP_METHODS::contains)
				.forEach(method -> result.add(method.toUpperCase() + " " + path)));
		return result;
	}

	/** Every {@code $ref} that is not a local pointer to an existing node of the document. */
	static List<String> unresolvedRefs(Map<String, Object> document) {
		List<String> refs = new ArrayList<>();
		collectRefs(document, refs);
		assertThat(refs).as("document has internal references").isNotEmpty();
		return refs.stream().filter(ref -> !resolves(document, ref)).distinct().toList();
	}

	private static void collectRefs(Object node, List<String> refs) {
		if (node instanceof Map<?, ?> map) {
			map.forEach((key, value) -> {
				if ("$ref".equals(key) && value instanceof String ref) {
					refs.add(ref);
				} else {
					collectRefs(value, refs);
				}
			});
		} else if (node instanceof List<?> list) {
			list.forEach(item -> collectRefs(item, refs));
		}
	}

	private static boolean resolves(Map<String, Object> document, String ref) {
		if (!ref.startsWith("#/")) {
			return false;
		}
		Object current = document;
		for (String token : ref.substring(2).split("/")) {
			String key = token.replace("~1", "/").replace("~0", "~");
			if (!(current instanceof Map<?, ?> map) || !map.containsKey(key)) {
				return false;
			}
			current = map.get(key);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object value) {
		assertThat(value).isInstanceOf(Map.class);
		return (Map<String, Object>) value;
	}

	private Map<String, Object> operation(String path, String method) {
		return asMap(asMap(asMap(docs.get("paths")).get(path)).get(method));
	}

	private Map<String, Object> responses(String path, String method) {
		return asMap(operation(path, method).get("responses"));
	}

	/** Operation-level security if declared, otherwise the document-level default. */
	private Object effectiveSecurity(String path, String method) {
		Map<String, Object> op = operation(path, method);
		return op.containsKey("security") ? op.get("security") : docs.get("security");
	}
}
