package group16.webapp.modules.availability;

import com.jayway.jsonpath.JsonPath;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import group16.webapp.modules.availability.repository.AvailabilityRuleRepository;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityRuleControllerTests extends AuthenticatedMvcTest {

    @Autowired
    private AvailabilityRuleRepository ruleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void cleanUpRules() {
        ruleRepository.deleteAll();
    }

    @Test
    void crudLifecycleAndOwnerIsolation() throws Exception {
        User otherUser = userRepository.save(new User("other_avail@example.com", "$2a$10$hash", "Other", null));
        String otherToken = jwtService.issue(otherUser).value();

        // Create rule as caller with default active = true
        String createBody = "{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"12:00\"}";
        String response = callAuthenticated(HttpMethod.POST, "/v1/availability-rules", createBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dayOfWeek").value(1))
                .andExpect(jsonPath("$.data.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("12:00:00"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Number ruleIdNum = JsonPath.read(response, "$.data.id");
        long ruleId = ruleIdNum.longValue();

        // Caller sees 1 rule; otherUser sees 0 rules
        callAuthenticated(HttpMethod.GET, "/v1/availability-rules", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        callWithToken(otherToken, HttpMethod.GET, "/v1/availability-rules", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // otherUser cannot update or delete caller's rule (404 RESOURCE_NOT_FOUND)
        String updateBody = "{\"dayOfWeek\":2,\"startTime\":\"14:00\",\"endTime\":\"17:00\",\"active\":false}";
        callWithToken(otherToken, HttpMethod.PUT, "/v1/availability-rules/" + ruleId, updateBody)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        callWithToken(otherToken, HttpMethod.DELETE, "/v1/availability-rules/" + ruleId, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Caller updates own rule
        callAuthenticated(HttpMethod.PUT, "/v1/availability-rules/" + ruleId, updateBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dayOfWeek").value(2))
                .andExpect(jsonPath("$.data.startTime").value("14:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("17:00:00"))
                .andExpect(jsonPath("$.data.active").value(false));

        // Caller deletes own rule
        callAuthenticated(HttpMethod.DELETE, "/v1/availability-rules/" + ruleId, null)
                .andExpect(status().isNoContent());

        callAuthenticated(HttpMethod.GET, "/v1/availability-rules", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void updateWithoutActiveKeepsTheCurrentValue() throws Exception {
        String created = callAuthenticated(HttpMethod.POST, "/v1/availability-rules",
                "{\"dayOfWeek\":4,\"startTime\":\"08:00\",\"endTime\":\"10:00\",\"active\":false}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.active").value(false))
                .andReturn().getResponse().getContentAsString();
        long ruleId = ((Number) JsonPath.read(created, "$.data.id")).longValue();

        // active omitted: a deactivated rule must stay deactivated (D-1).
        callAuthenticated(HttpMethod.PUT, "/v1/availability-rules/" + ruleId,
                "{\"dayOfWeek\":4,\"startTime\":\"08:30\",\"endTime\":\"10:00\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startTime").value("08:30:00"))
                .andExpect(jsonPath("$.data.active").value(false));

        // An explicit value still applies.
        callAuthenticated(HttpMethod.PUT, "/v1/availability-rules/" + ruleId,
                "{\"dayOfWeek\":4,\"startTime\":\"08:30\",\"endTime\":\"10:00\",\"active\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void equalOrReversedTimeRangeReturns400InvalidTimeRange() throws Exception {
        callAuthenticated(HttpMethod.POST, "/v1/availability-rules",
                "{\"dayOfWeek\":3,\"startTime\":\"10:00\",\"endTime\":\"10:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));

        callAuthenticated(HttpMethod.POST, "/v1/availability-rules",
                "{\"dayOfWeek\":3,\"startTime\":\"15:00\",\"endTime\":\"12:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
    }

    @Test
    void anonymousCallsReturn401() throws Exception {
        callAnonymous(HttpMethod.GET, "/v1/availability-rules", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        callAnonymous(HttpMethod.POST, "/v1/availability-rules",
                "{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"12:00\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    private ResultActions callWithToken(String bearerToken, HttpMethod method, String uri, String jsonBody) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        if (jsonBody != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return mockMvc.perform(builder);
    }
}