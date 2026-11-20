package group16.webapp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

/** UC-05 register / login / logout through the real security filter chain (H2, test profile). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

	private static final String PASSWORD = "Password123@";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void cleanUsers() {
		userRepository.deleteAll();
	}

	// ---------- register ----------

	@Test
	void registerReturns201WithNormalizedEmailAndHashedPassword() throws Exception {
		register("Anna@Example.COM", PASSWORD, "  Anna Nguyen ", null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber())
				.andExpect(jsonPath("$.data.email").value("anna@example.com"))
				.andExpect(jsonPath("$.data.fullName").value("Anna Nguyen"))
				.andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
				.andExpect(jsonPath("$.data.planningStart").value("08:00:00"))
				.andExpect(jsonPath("$.data.planningEnd").value("18:00:00"))
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist())
				.andExpect(content().string(not(containsString(PASSWORD))));

		User user = userRepository.findByEmail("anna@example.com").orElseThrow();
		assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
	}

	@Test
	void registerAcceptsValidTimezone() throws Exception {
		register("tz@example.com", PASSWORD, "Tz", "Europe/Paris")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.timezone").value("Europe/Paris"));
	}

	@Test
	void registerWithExistingEmailIgnoringCaseReturns409() throws Exception {
		register("anna@example.com", PASSWORD, "Anna", null).andExpect(status().isCreated());

		register("ANNA@example.com", PASSWORD, "Other", null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
		assertThat(userRepository.count()).isEqualTo(1);
	}

	@Test
	void registerWithInvalidFieldsReturns400() throws Exception {
		register("not-an-email", PASSWORD, "Anna", null)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.email").exists());
		// Strict: surrounding spaces are rejected by @Email (the frontend trims before sending).
		register(" a@example.com ", PASSWORD, "Anna", null)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.email").exists());
		register("a@example.com", "short1", "Anna", null)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.password").exists());
		register("a@example.com", "x".repeat(73), "Anna", null)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.password").exists());
		register("a@example.com", PASSWORD, "   ", null)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.fullName").exists());
		register("a@example.com", PASSWORD, "x".repeat(101), null)
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.fullName").exists());
		register("a@example.com", PASSWORD, "Anna", "Mars/Olympus")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.data.timezone").exists());
		assertThat(userRepository.count()).isZero();
	}

	@Test
	void registerRejectsPasswordOver72Bytes() throws Exception {
		// 30 characters but 90 UTF-8 bytes: passes @Size, rejected by the BCrypt byte limit.
		register("a@example.com", "ệ".repeat(30), "Anna", null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data.password").exists());
	}

	// ---------- login ----------

	@Test
	void loginReturnsTokenValidFor24Hours() throws Exception {
		register("anna@example.com", PASSWORD, "Anna", null).andExpect(status().isCreated());
		Long userId = userRepository.findByEmail("anna@example.com").orElseThrow().getId();

		String body = login(" ANNA@example.com ", PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresAt").exists())
				.andExpect(jsonPath("$.data.user.email").value("anna@example.com"))
				.andReturn().getResponse().getContentAsString();

		Jwt jwt = jwtDecoder.decode(JsonPath.read(body, "$.data.accessToken"));
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(userId));
		assertThat(jwt.getClaimAsString("email")).isEqualTo("anna@example.com");
		assertThat(jwt.getClaims()).doesNotContainKey("roles");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(24));
	}

	@Test
	void issuedTokenPassesSecurityAndLogoutReturns204() throws Exception {
		register("anna@example.com", PASSWORD, "Anna", null).andExpect(status().isCreated());
		String token = accessToken("anna@example.com", PASSWORD);

		mockMvc.perform(post("/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		// Authenticated request to an unmapped path reaches MVC (404), i.e. it was not rejected with 401.
		mockMvc.perform(get("/v1/not-mapped").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void wrongPasswordUnknownEmailAndInactiveUserGetTheSame401() throws Exception {
		register("anna@example.com", PASSWORD, "Anna", null).andExpect(status().isCreated());
		register("off@example.com", PASSWORD, "Off", null).andExpect(status().isCreated());
		User inactive = userRepository.findByEmail("off@example.com").orElseThrow();
		inactive.deactivate();
		userRepository.save(inactive);

		String wrongPassword = expectInvalidCredentials(login("anna@example.com", "WrongPassword1"));
		String unknownEmail = expectInvalidCredentials(login("nobody@example.com", PASSWORD));
		String inactiveUser = expectInvalidCredentials(login("off@example.com", PASSWORD));

		assertThat(unknownEmail).isEqualTo(wrongPassword);
		assertThat(inactiveUser).isEqualTo(wrongPassword);
	}

	@Test
	void loginWithBlankFieldsReturns400() throws Exception {
		login("", "").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.data.email").exists())
				.andExpect(jsonPath("$.data.password").exists());
	}

	// ---------- helpers ----------

	private ResultActions register(String email, String password, String fullName, String timezone) throws Exception {
		String tz = timezone == null ? "null" : "\"" + timezone + "\"";
		String json = "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\",\"timezone\":%s}"
				.formatted(email, password, fullName, tz);
		return mockMvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions login(String email, String password) throws Exception {
		String json = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
		return mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String accessToken(String email, String password) throws Exception {
		String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.accessToken");
	}

	/** Returns the message so callers can compare that all failure causes look identical. */
	private String expectInvalidCredentials(ResultActions result) throws Exception {
		String body = result.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.message");
	}
}
