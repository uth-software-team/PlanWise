package group16.webapp.modules.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Contract tests for availability, fixed-event and calendar endpoints (401, 400 validation, and OpenAPI schemas).
 */
class AvailabilityControllerStubTests extends AuthenticatedMvcTest {

    private static final String RULE = "{\"dayOfWeek\":1,\"startTime\":\"09:00\",\"endTime\":\"17:00\"}";
    private static final String ONCE = "{\"title\":\"Lecture\",\"note\":\"Room B\",\"recurrenceType\":\"ONCE\","
            + "\"startAt\":\"2026-11-24T02:00:00Z\",\"endAt\":\"2026-11-24T04:00:00Z\"}";
    private static final String WEEKLY = "{\"title\":\"Gym\",\"recurrenceType\":\"WEEKLY\",\"dayOfWeek\":3,"
            + "\"startTime\":\"18:00\",\"endTime\":\"19:30\",\"effectiveFrom\":\"2026-11-23\","
            + "\"effectiveUntil\":\"2026-12-31\"}";
    private static final String CALENDAR = "/v1/calendar?start=2026-11-23T00:00:00Z&end=2026-11-30T00:00:00Z";

    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/v1/fixed-events", null),
                Arguments.of(HttpMethod.POST, "/v1/fixed-events", ONCE),
                Arguments.of(HttpMethod.POST, "/v1/fixed-events", WEEKLY),
                Arguments.of(HttpMethod.PUT, "/v1/fixed-events/1", WEEKLY),
                Arguments.of(HttpMethod.DELETE, "/v1/fixed-events/1", null),
                Arguments.of(HttpMethod.GET, CALENDAR, null));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void anonymousCallReturns401(HttpMethod method, String uri, String body) throws Exception {
        callAnonymous(method, uri, body)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("rule day 0", HttpMethod.POST, "/v1/availability-rules", RULE.replace(":1,", ":0,")),
                Arguments.of("rule day 8", HttpMethod.POST, "/v1/availability-rules", RULE.replace(":1,", ":8,")),
                Arguments.of("rule without day", HttpMethod.POST, "/v1/availability-rules",
                        RULE.replace("\"dayOfWeek\":1,", "")),
                Arguments.of("rule ending at 24:00", HttpMethod.POST, "/v1/availability-rules",
                        RULE.replace("17:00", "24:00")),
                Arguments.of("rule with malformed time", HttpMethod.PUT, "/v1/availability-rules/1",
                        RULE.replace("09:00", "9am")),
                Arguments.of("rule without end", HttpMethod.POST, "/v1/availability-rules",
                        "{\"dayOfWeek\":1,\"startTime\":\"09:00\"}"),
                Arguments.of("event without recurrence type", HttpMethod.POST, "/v1/fixed-events",
                        ONCE.replace("\"recurrenceType\":\"ONCE\",", "")),
                Arguments.of("event with unknown recurrence", HttpMethod.POST, "/v1/fixed-events",
                        ONCE.replace("\"ONCE\"", "\"DAILY\"")),
                Arguments.of("ONCE without endAt", HttpMethod.POST, "/v1/fixed-events",
                        ONCE.replace(",\"endAt\":\"2026-11-24T04:00:00Z\"", "")),
                Arguments.of("WEEKLY without dayOfWeek", HttpMethod.POST, "/v1/fixed-events",
                        WEEKLY.replace("\"dayOfWeek\":3,", "")),
                Arguments.of("WEEKLY without startTime", HttpMethod.PUT, "/v1/fixed-events/1",
                        WEEKLY.replace("\"startTime\":\"18:00\",", "")),
                Arguments.of("WEEKLY day 8", HttpMethod.POST, "/v1/fixed-events", WEEKLY.replace(":3,", ":8,")),
                Arguments.of("WEEKLY ending at 24:00", HttpMethod.POST, "/v1/fixed-events",
                        WEEKLY.replace("19:30", "24:00")),
                Arguments.of("blank event title", HttpMethod.POST, "/v1/fixed-events", ONCE.replace("Lecture", "  ")),
                Arguments.of("tab/newline event title", HttpMethod.POST, "/v1/fixed-events",
                        ONCE.replace("Lecture", "\\t\\n")),
                Arguments.of("malformed effective date", HttpMethod.POST, "/v1/fixed-events",
                        WEEKLY.replace("2026-12-31", "end of year")),
                Arguments.of("calendar without end", HttpMethod.GET, "/v1/calendar?start=2026-11-23T00:00:00Z", null),
                Arguments.of("calendar with malformed start", HttpMethod.GET,
                        "/v1/calendar?start=monday&end=2026-11-30T00:00:00Z", null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void invalidRequestReturns400(String caseName, HttpMethod method, String uri, String body) throws Exception {
        callAuthenticated(method, uri, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void openApiDocumentsMethodsSuccessCodesAndSchemas() throws Exception {
        apiDocs()
                .andExpect(jsonPath("$.paths['/v1/availability-rules'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseListAvailabilityRuleResponse"))
                .andExpect(jsonPath("$.paths['/v1/availability-rules'].post.responses['201'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseAvailabilityRuleResponse"))
                .andExpect(jsonPath("$.paths['/v1/availability-rules'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/availability-rules/{id}'].put.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseAvailabilityRuleResponse"))
                .andExpect(jsonPath("$.paths['/v1/availability-rules/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/v1/availability-rules/{id}'].delete.responses['204'].content").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/fixed-events'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseListFixedEventResponse"))
                .andExpect(jsonPath("$.paths['/v1/fixed-events'].post.responses['201'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseFixedEventResponse"))
                .andExpect(jsonPath("$.paths['/v1/fixed-events'].post.responses['200']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/fixed-events/{id}'].put.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseFixedEventResponse"))
                .andExpect(jsonPath("$.paths['/v1/fixed-events/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/v1/fixed-events/{id}'].delete.responses['204'].content").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/calendar'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ApiResponseCalendarResponse"))
                .andExpect(jsonPath("$.paths['/v1/calendar'].get.parameters[?(@.name=='start')].required")
                        .value(contains(true)))
                .andExpect(jsonPath("$.paths['/v1/calendar'].get.parameters[?(@.name=='end')].required")
                        .value(contains(true)));
    }

    @Test
    void openApiDocumentsStableEnumsAndWarnings() throws Exception {
        apiDocs()
                .andExpect(jsonPath("$.components.schemas.FixedEventRequest.properties.recurrenceType.enum")
                        .value(contains("ONCE", "WEEKLY")))
                .andExpect(jsonPath("$.components.schemas.FixedEventResponse.properties.recurrenceType.enum")
                        .value(contains("ONCE", "WEEKLY")))
                .andExpect(jsonPath("$.components.schemas.CalendarFixedEvent.properties.recurrenceType.enum")
                        .value(contains("ONCE", "WEEKLY")))
                .andExpect(jsonPath("$.components.schemas.FixedEventResponse.properties.warnings.items.enum")
                        .value(contains("SESSION_AFFECTED")))
                .andExpect(jsonPath("$.components.schemas.CalendarSession.properties.status.enum")
                        .value(contains("CONFIRMED", "COMPLETED")))
                .andExpect(jsonPath("$.components.schemas.FixedEventRequest.properties.requiredRecurrenceFieldsPresent")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CalendarFixedEvent.properties.note").doesNotExist());
    }

    @Test
    void generatedFixedEventSchemaRequiresFieldsOfTheRecurrenceType() throws Exception {
        String docs = apiDocs().andReturn().getResponse().getContentAsString();
        Map<String, Object> schema = JsonPath.read(docs, "$.components.schemas.FixedEventRequest");

        Map<String, Object> once = Map.of("title", "Lecture", "recurrenceType", "ONCE",
                "startAt", "2026-11-24T02:00:00Z", "endAt", "2026-11-24T04:00:00Z");
        Map<String, Object> weekly = Map.of("title", "Gym", "recurrenceType", "WEEKLY",
                "dayOfWeek", 3, "startTime", "18:00", "endTime", "19:30");

        assertThat(satisfies(schema, once)).as("valid ONCE").isTrue();
        assertThat(satisfies(schema, weekly)).as("valid WEEKLY").isTrue();
        assertThat(satisfies(schema, Map.of("title", "Lecture", "recurrenceType", "ONCE",
                "startAt", "2026-11-24T02:00:00Z"))).as("ONCE without endAt").isFalse();
        assertThat(satisfies(schema, Map.of("title", "Gym", "recurrenceType", "WEEKLY",
                "dayOfWeek", 3, "endTime", "19:30"))).as("WEEKLY without startTime").isFalse();
        assertThat(satisfies(schema, Map.of("title", "Gym", "recurrenceType", "ONCE",
                "dayOfWeek", 3, "startTime", "18:00", "endTime", "19:30"))).as("ONCE with weekly fields only").isFalse();
        assertThat(satisfies(schema, Map.of("recurrenceType", "ONCE",
                "startAt", "2026-11-24T02:00:00Z", "endAt", "2026-11-24T04:00:00Z"))).as("missing title").isFalse();
    }

    @SuppressWarnings("unchecked")
    private static boolean satisfies(Map<String, Object> schema, Map<String, Object> body) {
        List<String> required = (List<String>) schema.getOrDefault("required", List.of());
        if (!body.keySet().containsAll(required)) {
            return false;
        }
        if (!schema.containsKey("oneOf")) {
            return true;
        }
        int matches = 0;
        for (Map<String, Object> branch : (List<Map<String, Object>>) schema.get("oneOf")) {
            assertThat(branch.keySet()).isSubsetOf("required", "properties");
            if (branchMatches(branch, body)) {
                matches++;
            }
        }
        return matches == 1;
    }

    @SuppressWarnings("unchecked")
    private static boolean branchMatches(Map<String, Object> branch, Map<String, Object> body) {
        List<String> required = (List<String>) branch.getOrDefault("required", List.of());
        if (!body.keySet().containsAll(required)) {
            return false;
        }
        Map<String, Map<String, Object>> properties =
                (Map<String, Map<String, Object>>) branch.getOrDefault("properties", Map.of());
        for (Map.Entry<String, Map<String, Object>> property : properties.entrySet()) {
            assertThat(property.getValue().keySet()).isSubsetOf("enum");
            List<Object> allowed = (List<Object>) property.getValue().get("enum");
            if (body.containsKey(property.getKey()) && !allowed.contains(body.get(property.getKey()))) {
                return false;
            }
        }
        return true;
    }
}