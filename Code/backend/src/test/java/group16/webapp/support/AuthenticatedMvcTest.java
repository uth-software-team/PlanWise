package group16.webapp.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base class for MockMvc tests that call the API as a real, active user with a real JWT (H2, test profile).
 * Same context configuration as the other @SpringBootTest + @AutoConfigureMockMvc tests, so it is reused.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AuthenticatedMvcTest {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	private String token;

	@BeforeEach
	void createCaller() {
		userRepository.deleteAll();
		User caller = userRepository.save(new User("caller@example.com", "$2a$10$hash", "Caller", null));
		token = jwtService.issue(caller).value();
	}

	/** Calls the endpoint with the caller's token; {@code jsonBody} may be null. */
	protected ResultActions callAuthenticated(HttpMethod method, String uri, String jsonBody) throws Exception {
		return mockMvc.perform(withBody(request(method, uri), jsonBody)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	/** Calls the endpoint without any token; {@code jsonBody} may be null. */
	protected ResultActions callAnonymous(HttpMethod method, String uri, String jsonBody) throws Exception {
		return mockMvc.perform(withBody(request(method, uri), jsonBody));
	}

	/** The generated OpenAPI document (public endpoint). */
	protected ResultActions apiDocs() throws Exception {
		return mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	private static MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder builder, String jsonBody) {
		return jsonBody == null ? builder : builder.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
	}
}
