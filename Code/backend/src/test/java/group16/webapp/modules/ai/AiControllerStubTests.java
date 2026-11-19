package group16.webapp.modules.ai;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;

/** GR-01 contract: the AI parse stub answers 501, validates its input and is documented in OpenAPI. */
class AiControllerStubTests extends AuthenticatedMvcTest {

	private static final String PARSE = "/v1/ai/tasks/parse";

	@Test
	void authenticatedValidCallReturns501() throws Exception {
		callAuthenticated(HttpMethod.POST, PARSE, "{\"text\":\"Write the report in 90 minutes by Friday 18:00\"}")
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
	}

	@Test
	void anonymousCallReturns401() throws Exception {
		callAnonymous(HttpMethod.POST, PARSE, "{\"text\":\"Write the report\"}")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"text\":null}", "{\"text\":\"   \"}", "{\"text\":\"\\t\\n\"}"})
	void blankTextReturns400(String body) throws Exception {
		callAuthenticated(HttpMethod.POST, PARSE, body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.data.text").exists());
	}

	@Test
	void openApiDocumentsParseEndpointAndDraftEnums() throws Exception {
		apiDocs()
				.andExpect(jsonPath("$.paths['/v1/ai/tasks/parse'].post.responses['200'].content['*/*'].schema['$ref']")
						.value("#/components/schemas/ApiResponseTaskDraftResponse"))
				.andExpect(jsonPath("$.components.schemas.TaskDraftResponse.properties.priority.enum")
						.value(contains("LOW", "MEDIUM", "HIGH", "URGENT")))
				.andExpect(jsonPath("$.components.schemas.TaskDraftResponse.properties.category.enum")
						.value(contains("STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER")));
	}
}
