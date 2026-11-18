package group16.webapp.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Every error path returns the ApiResponse envelope with the design-doc error code and no internal details. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTests {

	private static final String BASE = "/v1/test-probe";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void invalidBodyReturns400WithFieldErrors() throws Exception {
		expectError(mockMvc.perform(post(BASE + "/body").with(jwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"  \",\"estimatedDurationMinutes\":0}")),
				400, "INVALID_INPUT")
				.andExpect(jsonPath("$.data.title").exists())
				.andExpect(jsonPath("$.data.estimatedDurationMinutes").exists());
	}

	@Test
	void malformedJsonReturns400() throws Exception {
		expectError(mockMvc.perform(post(BASE + "/body").with(jwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not json")),
				400, "INVALID_INPUT");
	}

	@Test
	void invalidRequestParamReturns400() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/param").param("size", "0").with(jwt())), 400, "INVALID_INPUT")
				.andExpect(jsonPath("$.data.size").exists());
	}

	@Test
	void missingAndMistypedRequestParamReturn400() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/param").with(jwt())), 400, "INVALID_INPUT");
		expectError(mockMvc.perform(get(BASE + "/param").param("size", "abc").with(jwt())), 400, "INVALID_INPUT");
	}

	@Test
	void unsupportedMethodReturns405WithAllowHeader() throws Exception {
		expectError(mockMvc.perform(post(BASE + "/app").with(jwt())), 405, "METHOD_NOT_ALLOWED")
				.andExpect(header().string("Allow", containsString("GET")));
	}

	@Test
	void postToUnmappedPathDoesNotReturn500() throws Exception {
		// Regression: previously fell through to the generic handler (500).
		mockMvc.perform(post("/v1/does-not-exist").with(jwt()))
				.andExpect(status().is4xxClientError())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void unsupportedContentTypeReturns415() throws Exception {
		expectError(mockMvc.perform(post(BASE + "/body").with(jwt())
						.contentType(MediaType.TEXT_PLAIN)
						.content("hello")),
				415, "UNSUPPORTED_MEDIA_TYPE");
	}

	@Test
	void appExceptionKeepsItsCodeAndStatus() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/app").with(jwt())), 409, "EMAIL_ALREADY_EXISTS");
	}

	@Test
	void constraintViolationReturns409WithConflicts() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/conflicts").with(jwt())), 409, "CONSTRAINT_VIOLATION")
				.andExpect(jsonPath("$.data.conflicts.length()").value(2))
				.andExpect(jsonPath("$.data.conflicts[0].sessionId").value(11))
				.andExpect(jsonPath("$.data.conflicts[0].code").value("SESSION_OVERLAP"))
				.andExpect(jsonPath("$.data.conflicts[0].message").value("Overlaps session 7"))
				.andExpect(jsonPath("$.data.conflicts[1].code").value("DEADLINE_EXCEEDED"));
	}

	@Test
	void notImplementedStubReturns501() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/not-implemented").with(jwt())), 501, "NOT_IMPLEMENTED")
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void aiUnavailableReturns503WithoutData() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/ai").with(jwt())), 503, "AI_UNAVAILABLE")
				.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void optimisticLockReturns409StaleVersion() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/stale").with(jwt())), 409, "STALE_VERSION");
	}

	@Test
	void dataIntegrityViolationReturns409WithoutSqlDetails() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/integrity").with(jwt())), 409, "DATA_CONFLICT")
				.andExpect(content().string(not(containsString("uk_users_email"))));
	}

	@Test
	void unexpectedExceptionReturns500WithoutInternalMessage() throws Exception {
		expectError(mockMvc.perform(get(BASE + "/boom").with(jwt())), 500, "INTERNAL_ERROR")
				.andExpect(jsonPath("$.message").value("Internal server error"))
				.andExpect(content().string(not(containsString("secret internal detail"))));
	}

	@Test
	void unknownPathReturns404() throws Exception {
		expectError(mockMvc.perform(get("/v1/does-not-exist").with(jwt())), 404, "RESOURCE_NOT_FOUND");
	}

	private ResultActions expectError(ResultActions result, int status, String code) throws Exception {
		return result
				.andExpect(status().is(status))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.timestamp").exists());
	}

}
