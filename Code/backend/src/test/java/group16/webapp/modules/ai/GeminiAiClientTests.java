package group16.webapp.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.ai.client.AiResponse;
import group16.webapp.modules.ai.client.GeminiAiClient;
import group16.webapp.modules.ai.client.TaskDraftJsonSchema;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** AI-01: request shape, response handling and error mapping of GeminiAiClient (no network, fake key). */
class GeminiAiClientTests {

	private static final String BASE_URL = "https://gemini.test";
	private static final String MODEL = "gemini-3.5-flash-lite";
	private static final String KEY = "test-key";
	private static final String URL = BASE_URL + "/v1beta/models/" + MODEL + ":generateContent";
	private static final String PROMPT = "Ôn tập chương 3 môn Cơ sở dữ liệu trước thứ Sáu, khoảng 2 tiếng";
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private MockRestServiceServer server;
	private GeminiAiClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new GeminiAiClient(builder.build(), MODEL, KEY);
	}

	// ---------- request ----------

	@Test
	void r1SendsPostToGenerateContentWithKeyInHeaderOnly() {
		server.expect(requestTo(URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", KEY))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(request -> assertThat(request.getURI().getRawQuery()).isNull())
				.andExpect(request -> assertThat(request.getURI().toString()).doesNotContain(KEY))
				.andRespond(withSuccess(ok(List.of(textPart("{}")), 10), MediaType.APPLICATION_JSON));

		client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA);

		server.verify();
	}

	@Test
	void r2BodyAsksForJsonWithTheSchemaAndOneUserTurn() {
		String schemaJson = JSON.writeValueAsString(TaskDraftJsonSchema.SCHEMA);
		server.expect(requestTo(URL))
				.andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
				.andExpect(request -> assertThat(JSON.readTree(bodyOf(request))
						.path("generationConfig").path("responseJsonSchema"))
						.isEqualTo(JSON.readTree(schemaJson)))
				.andExpect(jsonPath("$.contents.length()").value(1))
				.andExpect(jsonPath("$.contents[0].role").value("user"))
				.andExpect(jsonPath("$.contents[0].parts.length()").value(1))
				.andExpect(jsonPath("$.contents[0].parts[0].text").value(PROMPT))
				.andRespond(withSuccess(ok(List.of(textPart("{}")), 10), MediaType.APPLICATION_JSON));

		client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA);

		server.verify();
	}

	@Test
	void r3SendsNoSamplingOrThinkingParameters() {
		server.expect(requestTo(URL))
				.andExpect(jsonPath("$.generationConfig.temperature").doesNotExist())
				.andExpect(jsonPath("$.generationConfig.topP").doesNotExist())
				.andExpect(jsonPath("$.generationConfig.topK").doesNotExist())
				.andExpect(jsonPath("$.generationConfig.thinkingConfig").doesNotExist())
				.andExpect(jsonPath("$.temperature").doesNotExist())
				.andRespond(withSuccess(ok(List.of(textPart("{}")), 10), MediaType.APPLICATION_JSON));

		client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA);

		server.verify();
	}

	// ---------- success ----------

	@Test
	void s1JoinsNonThoughtPartsInOrderAndReadsTokenCount() {
		List<Map<String, Object>> parts = List.of(
				Map.of("text", "planning the answer", "thought", true),
				textPart("{\"title\":\"Ôn tập"),
				textPart(" chương 3\",\"estimatedDurationMinutes\":120}"));
		respond(ok(parts, 42));

		AiResponse response = client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA);

		assertThat(response.jsonText()).isEqualTo("{\"title\":\"Ôn tập chương 3\",\"estimatedDurationMinutes\":120}");
		assertThat(response.tokensUsed()).isEqualTo(42);
	}

	@Test
	void s2MissingTotalTokenCountGivesNullTokens() {
		respond(json(Map.of("candidates", List.of(candidate("STOP", List.of(textPart("{}")))))));
		assertThat(client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA).tokensUsed()).isNull();

		server.reset();
		respond(json(Map.of(
				"candidates", List.of(candidate("STOP", List.of(textPart("{}")))),
				"usageMetadata", Map.of("promptTokenCount", 5))));
		assertThat(client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA).tokensUsed()).isNull();
	}

	@Test
	void s3ValueErrorsInsideAnObjectAreLeftToAi02() {
		respond(ok(List.of(textPart("{\"estimatedDurationMinutes\":0}")), 7));

		assertThat(client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA).jsonText())
				.isEqualTo("{\"estimatedDurationMinutes\":0}");
	}

	// ---------- errors -> AI_UNAVAILABLE ----------

	@Test
	void e1TimeoutIsMappedToAiUnavailable() {
		// Error mapping only: MockRestServiceServer bypasses the real request factory (see GeminiAiClientTimeoutTests).
		server.expect(requestTo(URL)).andRespond(withException(new HttpTimeoutException("request timed out")));
		assertUnavailable();
	}

	@Test
	void e2TooManyRequestsIsAiUnavailable() {
		server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}"));
		assertUnavailable();
	}

	@ParameterizedTest
	@ValueSource(ints = {400, 401, 403, 404, 500, 503})
	void e3HttpErrorsAreAiUnavailable(int status) {
		server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)));
		assertUnavailable();
	}

	@ParameterizedTest
	@ValueSource(strings = {"not json", "", "[]", "{\"candidates\":"})
	void e4BodyThatIsNotAJsonObjectIsAiUnavailable(String body) {
		respond(body);
		assertUnavailable();
	}

	@Test
	void e5BlockedPromptIsAiUnavailable() {
		respond(json(Map.of("promptFeedback", Map.of("blockReason", "SAFETY"))));
		assertUnavailable();
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"candidates\":[]}", "{\"candidates\":null}"})
	void e6MissingCandidateIsAiUnavailable(String body) {
		respond(body);
		assertUnavailable();
	}

	@ParameterizedTest
	@ValueSource(strings = {"MAX_TOKENS", "SAFETY", "RECITATION", "OTHER"})
	void e7FinishReasonOtherThanStopIsAiUnavailable(String finishReason) {
		respond(json(Map.of("candidates", List.of(candidate(finishReason, List.of(textPart("{}")))))));
		assertUnavailable();
	}

	@Test
	void e7MissingFinishReasonIsAiUnavailable() {
		respond(json(Map.of("candidates", List.of(Map.of("content", Map.of("parts", List.of(textPart("{}"))))))));
		assertUnavailable();
	}

	@Test
	void e8OnlyThoughtPartsGiveEmptyTextAndAiUnavailable() {
		respond(ok(List.of(Map.of("text", "{\"title\":\"hidden\"}", "thought", true)), 3));
		assertUnavailable();
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void e8BlankTextIsAiUnavailable(String text) {
		respond(ok(List.of(textPart(text)), 3));
		assertUnavailable();
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "[]", "{} {}", "\"draft\"", "42", "{\"title\":"})
	void e9TextThatIsNotExactlyOneJsonObjectIsAiUnavailable(String text) {
		respond(ok(List.of(textPart(text)), 3));
		assertUnavailable();
	}

	// ---------- helpers ----------

	private void respond(String body) {
		server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}

	private void assertUnavailable() {
		assertThatThrownBy(() -> client.generateJson(PROMPT, TaskDraftJsonSchema.SCHEMA))
				.isInstanceOfSatisfying(AppException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_UNAVAILABLE));
		server.verify();
	}

	private static String ok(List<Map<String, Object>> parts, int totalTokens) {
		return json(Map.of(
				"candidates", List.of(candidate("STOP", parts)),
				"usageMetadata", Map.of("totalTokenCount", totalTokens)));
	}

	private static Map<String, Object> candidate(String finishReason, List<Map<String, Object>> parts) {
		return Map.of("content", Map.of("role", "model", "parts", parts), "finishReason", finishReason);
	}

	private static Map<String, Object> textPart(String text) {
		return Map.of("text", text);
	}

	private static String json(Object value) {
		return JSON.writeValueAsString(value);
	}

	private static String bodyOf(ClientHttpRequest request) {
		return ((MockClientHttpRequest) request).getBodyAsString();
	}
}
