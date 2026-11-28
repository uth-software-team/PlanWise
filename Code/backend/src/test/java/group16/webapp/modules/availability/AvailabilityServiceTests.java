package group16.webapp.modules.availability;

import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.entity.AvailabilityRuleEntity;
import group16.webapp.modules.availability.repository.AvailabilityRuleRepository;
import group16.webapp.modules.availability.service.AvailabilityService;
import group16.webapp.modules.availability.service.FixedEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTests {

    @Mock
    private AvailabilityRuleRepository ruleRepository;

    @Mock
    private FixedEventService fixedEventService;

    @Mock
    private ObjectProvider<UserApi> userApiProvider;

    @Mock
    private UserApi userApi;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        lenient().when(userApiProvider.getIfAvailable()).thenReturn(userApi);
        availabilityService = new AvailabilityService(ruleRepository, fixedEventService, userApiProvider);
    }

    @Test
    void expandsMergesOverlappingAndClipsToQueryWindow() {
        Long userId = 1L;
        when(userApi.findById(userId)).thenReturn(
                Optional.of(new UserSummary(userId, "u@test.com", "User", "Asia/Ho_Chi_Minh", true)));

        AvailabilityRuleEntity r1 = rule(1L, userId, 1, "08:00", "11:00", true);
        AvailabilityRuleEntity r2 = rule(2L, userId, 1, "10:00", "12:00", true);
        AvailabilityRuleEntity r3 = rule(3L, userId, 1, "12:00", "13:00", true);
        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(r1, r2, r3));

        Instant start = Instant.parse("2026-11-23T02:00:00Z");
        Instant end = Instant.parse("2026-11-23T07:00:00Z");

        List<TimeRange> intervals = availabilityService.availableIntervals(userId, start, end);

        assertThat(intervals).containsExactly(
                new TimeRange(Instant.parse("2026-11-23T02:00:00Z"), Instant.parse("2026-11-23T06:00:00Z"))
        );
    }

    @Test
    void handlesDstTransitionCorrectlyInAmericanTimezone() {
        Long userId = 2L;
        when(userApi.findById(userId)).thenReturn(
                Optional.of(new UserSummary(userId, "ny@test.com", "NY", "America/New_York", true)));

        AvailabilityRuleEntity sundayRule = rule(10L, userId, 7, "09:00", "11:00", true);
        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(sundayRule));

        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        Instant end = Instant.parse("2026-11-02T00:00:00Z");

        List<TimeRange> intervals = availabilityService.availableIntervals(userId, start, end);

        assertThat(intervals).containsExactly(
                new TimeRange(Instant.parse("2026-11-01T14:00:00Z"), Instant.parse("2026-11-01T16:00:00Z"))
        );
    }

    @Test
    void springForwardGapRuleYieldsNoIntervalAndSpanningRuleKeepsRealDuration() {
        Long userId = 3L;
        stubZone(userId, "America/New_York");
        // 2026-03-08 (Sunday): local 02:00 jumps to 03:00 EDT. Window = that local day: 05:00Z .. next 04:00Z.
        Instant start = Instant.parse("2026-03-08T05:00:00Z");
        Instant end = Instant.parse("2026-03-09T04:00:00Z");

        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(rule(20L, userId, 7, "02:00", "03:00", true)));
        assertThat(availabilityService.availableIntervals(userId, start, end)).isEmpty();

        // 01:00 EST (06:00Z) .. 04:00 EDT (08:00Z): two real hours on that day.
        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(rule(21L, userId, 7, "01:00", "04:00", true)));
        assertThat(availabilityService.availableIntervals(userId, start, end)).containsExactly(
                new TimeRange(Instant.parse("2026-03-08T06:00:00Z"), Instant.parse("2026-03-08T08:00:00Z")));
    }

    @Test
    void multiDayWindowExpandsEveryMatchingWeekday() {
        Long userId = 4L;
        stubZone(userId, "Asia/Ho_Chi_Minh");
        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(
                rule(30L, userId, 1, "09:00", "10:00", true),   // Monday
                rule(31L, userId, 3, "14:00", "15:00", true))); // Wednesday

        // Local week Mon 2026-11-23 00:00 .. Mon 2026-11-30 00:00 (+07).
        List<TimeRange> intervals = availabilityService.availableIntervals(userId,
                Instant.parse("2026-11-22T17:00:00Z"), Instant.parse("2026-11-29T17:00:00Z"));

        assertThat(intervals).containsExactly(
                new TimeRange(Instant.parse("2026-11-23T02:00:00Z"), Instant.parse("2026-11-23T03:00:00Z")),
                new TimeRange(Instant.parse("2026-11-25T07:00:00Z"), Instant.parse("2026-11-25T08:00:00Z")));
    }

    @Test
    void windowEndingExactlyAtLocalMidnightExcludesTheNextDay() {
        Long userId = 5L;
        stubZone(userId, "Asia/Ho_Chi_Minh");
        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(
                rule(40L, userId, 2, "22:00", "23:00", true),   // Tuesday
                rule(41L, userId, 3, "00:00", "01:00", true))); // Wednesday, starts exactly at the window end

        // Tue 2026-11-24 00:00 .. Wed 2026-11-25 00:00 (+07), half-open.
        List<TimeRange> intervals = availabilityService.availableIntervals(userId,
                Instant.parse("2026-11-23T17:00:00Z"), Instant.parse("2026-11-24T17:00:00Z"));

        assertThat(intervals).containsExactly(
                new TimeRange(Instant.parse("2026-11-24T15:00:00Z"), Instant.parse("2026-11-24T16:00:00Z")));
    }

    @Test
    void noActiveRulesYieldsNoInterval() {
        Long userId = 6L;
        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        assertThat(availabilityService.availableIntervals(userId,
                Instant.parse("2026-11-22T17:00:00Z"), Instant.parse("2026-11-29T17:00:00Z"))).isEmpty();
    }

    @Test
    void adjacentRulesMergeIntoOneInterval() {
        Long userId = 7L;
        stubZone(userId, "Asia/Ho_Chi_Minh");
        when(ruleRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(
                rule(50L, userId, 1, "10:00", "11:00", true),
                rule(51L, userId, 1, "11:00", "12:00", true)));

        List<TimeRange> intervals = availabilityService.availableIntervals(userId,
                Instant.parse("2026-11-22T17:00:00Z"), Instant.parse("2026-11-23T17:00:00Z"));

        assertThat(intervals).containsExactly(
                new TimeRange(Instant.parse("2026-11-23T03:00:00Z"), Instant.parse("2026-11-23T05:00:00Z")));
    }

    private void stubZone(Long userId, String zone) {
        when(userApi.findById(userId)).thenReturn(
                Optional.of(new UserSummary(userId, "u" + userId + "@test.com", "User", zone, true)));
    }

    private static AvailabilityRuleEntity rule(Long id, Long userId, int dayOfWeek, String start, String end, boolean active) {
        AvailabilityRuleEntity e = new AvailabilityRuleEntity();
        e.setId(id);
        e.setUserId(userId);
        e.setDayOfWeek((short) dayOfWeek);
        e.setStartTime(LocalTime.parse(start));
        e.setEndTime(LocalTime.parse(end));
        e.setActive(active);
        return e;
    }
}