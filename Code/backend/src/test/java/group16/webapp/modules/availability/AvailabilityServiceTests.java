package group16.webapp.modules.availability;

import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.entity.AvailabilityRuleEntity;
import group16.webapp.modules.availability.repository.AvailabilityRuleRepository;
import group16.webapp.modules.availability.service.AvailabilityService;
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
    private ObjectProvider<UserApi> userApiProvider;

    @Mock
    private UserApi userApi;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        lenient().when(userApiProvider.getIfAvailable()).thenReturn(userApi);
        availabilityService = new AvailabilityService(ruleRepository, userApiProvider);
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