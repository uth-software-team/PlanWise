package group16.webapp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/** UC-05 GET/PUT /v1/users/me with real JWTs (H2, test profile). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerTests {

	private static final String ME = "/v1/users/me";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JwtEncoder jwtEncoder;

	private User user;
	private String token;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		user = userRepository.save(new User("anna@example.com", "$2a$10$hash", "Anna", null));
		token = jwtService.issue(user).value();
	}

	// ---------- GET ----------

	@Test
	void getMeReturnsOwnProfile() throws Exception {
		mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(user.getId()))
				.andExpect(jsonPath("$.data.email").value("anna@example.com"))
				.andExpect(jsonPath("$.data.fullName").value("Anna"))
				.andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
				.andExpect(jsonPath("$.data.planningStart").value("08:00:00"))
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist());
	}

	@Test
	void getMeWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get(ME))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void inactiveOrMissingUserWithValidTokenReturns401() throws Exception {
		user.deactivate();
		userRepository.save(user);
		mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(tokenForSubject("999999"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	// ---------- PUT ----------

	@Test
	void updateChangesNameTimezoneAndPlanningWindow() throws Exception {
		Instant updatedBefore = user.getUpdatedAt();

		putMe("{\"fullName\":\"  Anna Tran \",\"timezone\":\"Europe/Paris\","
				+ "\"planningStart\":\"09:30\",\"planningEnd\":\"17:00\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fullName").value("Anna Tran"))
				.andExpect(jsonPath("$.data.timezone").value("Europe/Paris"))
				.andExpect(jsonPath("$.data.planningStart").value("09:30:00"))
				.andExpect(jsonPath("$.data.planningEnd").value("17:00:00"));

		User saved = userRepository.findById(user.getId()).orElseThrow();
		assertThat(saved.getFullName()).isEqualTo("Anna Tran");
		assertThat(saved.getTimezone()).isEqualTo("Europe/Paris");
		assertThat(saved.getPlanningStart()).isEqualTo(LocalTime.of(9, 30));
		assertThat(saved.getPlanningEnd()).isEqualTo(LocalTime.of(17, 0));
		assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(updatedBefore);
	}

	@Test
	void updateWithoutPlanningWindowKeepsCurrentWindow() throws Exception {
		putMe("{\"fullName\":\"Anna\",\"timezone\":\"Asia/Tokyo\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.timezone").value("Asia/Tokyo"))
				.andExpect(jsonPath("$.data.planningStart").value("08:00:00"))
				.andExpect(jsonPath("$.data.planningEnd").value("18:00:00"));
	}

	@Test
	void updateIgnoresUndeclaredFieldsLikeEmail() throws Exception {
		putMe("{\"fullName\":\"Anna\",\"timezone\":\"Asia/Ho_Chi_Minh\","
				+ "\"email\":\"hacker@example.com\",\"id\":42,\"password\":\"NewPassword1\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value("anna@example.com"))
				.andExpect(jsonPath("$.data.id").value(user.getId()));

		User saved = userRepository.findById(user.getId()).orElseThrow();
		assertThat(saved.getEmail()).isEqualTo("anna@example.com");
		assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hash");
	}

	@Test
	void updateWithInvalidFieldsReturns400() throws Exception {
		putMe("{\"fullName\":\"  \",\"timezone\":\"Asia/Ho_Chi_Minh\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.data.fullName").exists());
		putMe("{\"fullName\":\"%s\",\"timezone\":\"Asia/Ho_Chi_Minh\"}".formatted("x".repeat(101)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data.fullName").exists());
		putMe("{\"fullName\":\"Anna\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data.timezone").exists());
		putMe("{\"fullName\":\"Anna\",\"timezone\":\"Mars/Olympus\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.data.timezone").exists());
		putMe("{\"fullName\":\"Anna\",\"timezone\":\"Asia/Ho_Chi_Minh\",\"planningStart\":\"09:00\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.data.planningEnd").exists());
		putMe("{\"fullName\":\"Anna\",\"timezone\":\"Asia/Ho_Chi_Minh\",\"planningStart\":\"25:00\",\"planningEnd\":\"18:00\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));

		assertThat(userRepository.findById(user.getId()).orElseThrow().getFullName()).isEqualTo("Anna");
	}

	@Test
	void planningStartNotBeforeEndReturnsInvalidTimeRange() throws Exception {
		putMe("{\"fullName\":\"Anna\",\"timezone\":\"Asia/Ho_Chi_Minh\",\"planningStart\":\"18:00\",\"planningEnd\":\"08:00\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"))
				.andExpect(jsonPath("$.data.planningEnd").exists());
		putMe("{\"fullName\":\"Anna\",\"timezone\":\"Asia/Ho_Chi_Minh\",\"planningStart\":\"10:00\",\"planningEnd\":\"10:00\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
	}

	@Test
	void updateWithoutTokenReturns401() throws Exception {
		mockMvc.perform(put(ME).contentType(MediaType.APPLICATION_JSON)
						.content("{\"fullName\":\"Anna\",\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
				.andExpect(status().isUnauthorized());
	}

	// ---------- helpers ----------

	private ResultActions putMe(String json) throws Exception {
		return mockMvc.perform(put(ME).header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String tokenForSubject(String subject) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(subject)
				.issuedAt(now).expiresAt(now.plus(5, ChronoUnit.MINUTES)).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
