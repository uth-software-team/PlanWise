package group16.webapp.modules.availability;

import com.jayway.jsonpath.JsonPath;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import group16.webapp.modules.availability.api.AvailabilityApi;
import group16.webapp.modules.availability.api.CalendarApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.repository.FixedEventRepository;
import group16.webapp.modules.schedule.api.ScheduleApi;
import group16.webapp.modules.schedule.api.ScheduledSession;
import group16.webapp.modules.schedule.api.SessionStatus;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.api.TaskSummary;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FixedEventAndCalendarControllerTests extends AuthenticatedMvcTest {

    static final List<ScheduledSession> COMMITTED_SESSIONS = new CopyOnWriteArrayList<>();
    static final List<TaskSummary> TASK_SUMMARIES = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class StubScheduleAndTaskConfig {

        @Bean
        @Primary
        ScheduleApi testScheduleApi() {
            return (userId, start, end) -> COMMITTED_SESSIONS.stream()
                    .filter(s -> s.startAt().isBefore(end) && s.endAt().isAfter(start))
                    .toList();
        }

        @Bean
        @Primary
        TaskApi testTaskApi() {
            return new TaskApi() {
                @Override
                public List<group16.webapp.modules.task.api.SchedulableTask> schedulableTasks(Long userId, Collection<Long> ids) {
                    return List.of();
                }

                @Override
                public Long createGroupTask(Long groupId, Long creatorUserId, group16.webapp.modules.task.api.NewGroupTaskCommand cmd) {
                    return 1L;
                }

                @Override
                public List<TaskSummary> summaries(Collection<Long> taskIds) {
                    return TASK_SUMMARIES.stream()
                            .filter(t -> taskIds.contains(t.taskId()))
                            .toList();
                }

                @Override
                public Optional<Long> findActiveGroupTaskGroupId(Long taskId) {
                    return Optional.empty();
                }

                @Override
                public List<Long> groupTaskIds(Long groupId) {
                    return List.of();
                }
            };
        }
    }

    @Autowired
    private FixedEventRepository fixedEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AvailabilityApi availabilityApi;

    @Autowired
    private CalendarApi calendarApi;

    @BeforeEach
    void clearStubs() {
        COMMITTED_SESSIONS.clear();
        TASK_SUMMARIES.clear();
    }

    @AfterEach
    void cleanUpEvents() {
        fixedEventRepository.deleteAll();
    }

    @Test
    void fixedEventCrudValidationAndOwnerIsolation() throws Exception {
        User otherUser = userRepository.save(new User("other_fe@example.com", "$2a$10$hash", "Other", null));
        String otherToken = jwtService.issue(otherUser).value();

        // Create ONCE event
        String onceBody = "{\"title\":\"Dentist\",\"note\":\"Clinic\",\"recurrenceType\":\"ONCE\","
                + "\"startAt\":\"2026-11-24T02:00:00Z\",\"endAt\":\"2026-11-24T03:00:00Z\"}";
        String res = callAuthenticated(HttpMethod.POST, "/v1/fixed-events", onceBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Dentist"))
                .andExpect(jsonPath("$.data.recurrenceType").value("ONCE"))
                .andExpect(jsonPath("$.data.warnings", hasSize(0)))
                .andReturn().getResponse().getContentAsString();

        long eventId = ((Number) JsonPath.read(res, "$.data.id")).longValue();

        // Other user sees empty list and gets 404 on PUT/DELETE
        callWithToken(otherToken, HttpMethod.GET, "/v1/fixed-events", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        String weeklyUpdate = "{\"title\":\"Team Sync\",\"recurrenceType\":\"WEEKLY\",\"dayOfWeek\":2,"
                + "\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"effectiveFrom\":\"2026-11-23\",\"effectiveUntil\":\"2026-12-01\"}";
        callWithToken(otherToken, HttpMethod.PUT, "/v1/fixed-events/" + eventId, weeklyUpdate)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        callWithToken(otherToken, HttpMethod.DELETE, "/v1/fixed-events/" + eventId, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // Owner updates ONCE -> WEEKLY
        callAuthenticated(HttpMethod.PUT, "/v1/fixed-events/" + eventId, weeklyUpdate)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recurrenceType").value("WEEKLY"))
                .andExpect(jsonPath("$.data.dayOfWeek").value(2))
                .andExpect(jsonPath("$.data.startAt").doesNotExist());

        // Validation errors
        // Mixing WEEKLY fields into ONCE -> 400 INVALID_INPUT
        String mixedBody = "{\"title\":\"Bad\",\"recurrenceType\":\"ONCE\","
                + "\"startAt\":\"2026-11-24T02:00:00Z\",\"endAt\":\"2026-11-24T03:00:00Z\",\"dayOfWeek\":1}";
        callAuthenticated(HttpMethod.POST, "/v1/fixed-events", mixedBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        // Reversed range -> 400 INVALID_TIME_RANGE
        String reversedOnce = "{\"title\":\"Bad\",\"recurrenceType\":\"ONCE\","
                + "\"startAt\":\"2026-11-24T04:00:00Z\",\"endAt\":\"2026-11-24T03:00:00Z\"}";
        callAuthenticated(HttpMethod.POST, "/v1/fixed-events", reversedOnce)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));

        // effectiveFrom > effectiveUntil -> 400 INVALID_TIME_RANGE
        String badEffective = "{\"title\":\"Bad\",\"recurrenceType\":\"WEEKLY\",\"dayOfWeek\":1,"
                + "\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"effectiveFrom\":\"2026-12-10\",\"effectiveUntil\":\"2026-12-01\"}";
        callAuthenticated(HttpMethod.POST, "/v1/fixed-events", badEffective)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));

        // Owner deletes event
        callAuthenticated(HttpMethod.DELETE, "/v1/fixed-events/" + eventId, null)
                .andExpect(status().isNoContent());
    }

    @Test
    void overlappingConfirmedSessionEmitsSessionAffectedWarningAndPopulatesCalendar() throws Exception {
        User caller = userRepository.findAll().get(0);

        // Existing CONFIRMED session on Tue 2026-11-24 02:30Z - 03:30Z
        COMMITTED_SESSIONS.add(new ScheduledSession(
                100L, 50L,
                Instant.parse("2026-11-24T02:30:00Z"),
                Instant.parse("2026-11-24T03:30:00Z"),
                SessionStatus.CONFIRMED
        ));
        TASK_SUMMARIES.add(new TaskSummary(
                50L, null, "Write Architecture Doc",
                Instant.parse("2026-11-26T00:00:00Z"),
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS
        ));

        // Create ONCE event overlapping 02:00Z - 03:00Z -> returns SESSION_AFFECTED warning
        String onceBody = "{\"title\":\"Standup\",\"recurrenceType\":\"ONCE\","
                + "\"startAt\":\"2026-11-24T02:00:00Z\",\"endAt\":\"2026-11-24T03:00:00Z\"}";
        callAuthenticated(HttpMethod.POST, "/v1/fixed-events", onceBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.warnings", contains("SESSION_AFFECTED")));

        // Calendar endpoint returns both fixed event and committed session with resolved taskTitle
        Instant winStart = Instant.parse("2026-11-24T00:00:00Z");
        Instant winEnd = Instant.parse("2026-11-25T00:00:00Z");
        callAuthenticated(HttpMethod.GET, "/v1/calendar?start=" + winStart + "&end=" + winEnd, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.fixedEvents", hasSize(1)))
                .andExpect(jsonPath("$.data.fixedEvents[0].title").value("Standup"))
                .andExpect(jsonPath("$.data.sessions", hasSize(1)))
                .andExpect(jsonPath("$.data.sessions[0].taskTitle").value("Write Architecture Doc"))
                .andExpect(jsonPath("$.data.sessions[0].status").value("CONFIRMED"));

        // Invalid calendar range returns 400 INVALID_TIME_RANGE
        callAuthenticated(HttpMethod.GET, "/v1/calendar?start=" + winEnd + "&end=" + winStart, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));

        // AvailabilityApi.fixedEventOccurrences and CalendarApi.busyIntervals merge overlapping ranges
        List<TimeRange> fixedOccs = availabilityApi.fixedEventOccurrences(caller.getId(), winStart, winEnd);
        assertThat(fixedOccs).containsExactly(
                new TimeRange(Instant.parse("2026-11-24T02:00:00Z"), Instant.parse("2026-11-24T03:00:00Z"))
        );

        List<TimeRange> busy = calendarApi.busyIntervals(caller.getId(), winStart, winEnd);
        assertThat(busy).containsExactly(
                new TimeRange(Instant.parse("2026-11-24T02:00:00Z"), Instant.parse("2026-11-24T03:30:00Z"))
        );
    }

    @Test
    void calendarWindowIsCappedAt366Days() throws Exception {
        // D14: end - start <= 366 days at full Instant precision; one second over is 400.
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant exactly366 = start.plus(Duration.ofDays(366));

        callAuthenticated(HttpMethod.GET, "/v1/calendar?start=" + start + "&end=" + exactly366, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        callAuthenticated(HttpMethod.GET, "/v1/calendar?start=" + start + "&end=" + exactly366.plusSeconds(1), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));

        callAuthenticated(HttpMethod.GET, "/v1/calendar?start=" + start + "&end=" + start, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
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