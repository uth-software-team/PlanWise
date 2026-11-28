package group16.webapp.modules.availability;

import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.RecurrenceType;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.dto.CalendarResponse.CalendarFixedEvent;
import group16.webapp.modules.availability.dto.FixedEventRequest;
import group16.webapp.modules.availability.dto.FixedEventResponse;
import group16.webapp.modules.availability.entity.FixedEventEntity;
import group16.webapp.modules.availability.repository.FixedEventRepository;
import group16.webapp.modules.availability.service.FixedEventService;
import group16.webapp.modules.schedule.api.ScheduleApi;
import group16.webapp.modules.schedule.api.ScheduledSession;
import group16.webapp.modules.schedule.api.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** WEEKLY fixed-event expansion (AV-03) and the HC-09 SESSION_AFFECTED warning. */
@ExtendWith(MockitoExtension.class)
class FixedEventServiceTests {

    private static final Long USER_ID = 1L;

    @Mock
    private FixedEventRepository fixedEventRepository;
    @Mock
    private ObjectProvider<UserApi> userApiProvider;
    @Mock
    private ObjectProvider<ScheduleApi> scheduleApiProvider;
    @Mock
    private UserApi userApi;
    @Mock
    private ScheduleApi scheduleApi;

    private FixedEventService fixedEventService;

    @BeforeEach
    void setUp() {
        lenient().when(userApiProvider.getIfAvailable()).thenReturn(userApi);
        lenient().when(scheduleApiProvider.getIfAvailable()).thenReturn(scheduleApi);
        fixedEventService = new FixedEventService(fixedEventRepository, userApiProvider, scheduleApiProvider);
    }

    @Test
    void weeklyEventOccursOncePerMatchingWeekdayOverSeveralWeeks() {
        stubZone("Asia/Ho_Chi_Minh");
        when(fixedEventRepository.findByUserId(USER_ID)).thenReturn(List.of(weekly(1, "09:00", "10:00", null, null)));

        // Mon 2026-11-23 00:00 .. Mon 2026-12-14 00:00 (+07): three Mondays.
        List<TimeRange> occurrences = fixedEventService.expandMergedOccurrences(USER_ID,
                Instant.parse("2026-11-22T17:00:00Z"), Instant.parse("2026-12-13T17:00:00Z"));

        assertThat(occurrences).containsExactly(
                range("2026-11-23T02:00:00Z", "2026-11-23T03:00:00Z"),
                range("2026-11-30T02:00:00Z", "2026-11-30T03:00:00Z"),
                range("2026-12-07T02:00:00Z", "2026-12-07T03:00:00Z"));
    }

    @Test
    void effectiveRangeClampsOccurrencesInclusiveOnBothDates() {
        stubZone("Asia/Ho_Chi_Minh");
        // Both bounds are Mondays, so an exclusive bound would drop one of them.
        when(fixedEventRepository.findByUserId(USER_ID)).thenReturn(List.of(
                weekly(1, "09:00", "10:00", LocalDate.parse("2026-11-30"), LocalDate.parse("2026-12-07"))));

        // Four Mondays in the window: 11-23, 11-30, 12-07, 12-14.
        List<TimeRange> occurrences = fixedEventService.expandMergedOccurrences(USER_ID,
                Instant.parse("2026-11-22T17:00:00Z"), Instant.parse("2026-12-20T17:00:00Z"));

        assertThat(occurrences).containsExactly(
                range("2026-11-30T02:00:00Z", "2026-11-30T03:00:00Z"),
                range("2026-12-07T02:00:00Z", "2026-12-07T03:00:00Z"));
    }

    @Test
    void weeklyEventKeepsLocalWallClockAcrossDstChange() {
        stubZone("America/New_York");
        // US DST ends 2026-11-01: 09:00 local is 13:00Z before and 14:00Z after.
        when(fixedEventRepository.findByUserId(USER_ID)).thenReturn(List.of(weekly(1, "09:00", "10:00", null, null)));

        List<TimeRange> occurrences = fixedEventService.expandMergedOccurrences(USER_ID,
                Instant.parse("2026-10-26T04:00:00Z"), Instant.parse("2026-11-10T05:00:00Z"));

        assertThat(occurrences).containsExactly(
                range("2026-10-26T13:00:00Z", "2026-10-26T14:00:00Z"),
                range("2026-11-02T14:00:00Z", "2026-11-02T15:00:00Z"),
                range("2026-11-09T14:00:00Z", "2026-11-09T15:00:00Z"));
    }

    @Test
    void weeklyEventStartsAtFirstMatchingWeekdayWhenWindowAndEffectiveFromAreMidWeek() {
        stubZone("Asia/Ho_Chi_Minh");
        // Monday event, effective from Tue 2026-12-01; window Wed 2026-11-25 .. Wed 2026-12-16 (+07).
        when(fixedEventRepository.findByUserId(USER_ID)).thenReturn(List.of(
                weekly(1, "09:00", "10:00", LocalDate.parse("2026-12-01"), null)));

        List<TimeRange> occurrences = fixedEventService.expandMergedOccurrences(USER_ID,
                Instant.parse("2026-11-24T17:00:00Z"), Instant.parse("2026-12-15T17:00:00Z"));

        assertThat(occurrences).containsExactly(
                range("2026-12-07T02:00:00Z", "2026-12-07T03:00:00Z"),
                range("2026-12-14T02:00:00Z", "2026-12-14T03:00:00Z"));
    }

    @Test
    void weeklyCalendarSkipsOccurrenceInvertedByDstGap() {
        // US DST starts Sun 2027-03-14: 02:30 does not exist and becomes 03:30 EDT, after the 03:15 end.
        when(fixedEventRepository.findByUserId(USER_ID)).thenReturn(List.of(weekly(7, "02:30", "03:15", null, null)));

        List<CalendarFixedEvent> occurrences = fixedEventService.expandCalendarOccurrences(USER_ID,
                Instant.parse("2027-03-13T05:00:00Z"), Instant.parse("2027-03-23T04:00:00Z"),
                ZoneId.of("America/New_York"));

        assertThat(occurrences).extracting(CalendarFixedEvent::startAt, CalendarFixedEvent::endAt)
                .containsExactly(tuple(Instant.parse("2027-03-21T06:30:00Z"), Instant.parse("2027-03-21T07:15:00Z")));
    }

    @Test
    void weeklyEventOverlappingConfirmedSessionReturnsSessionAffectedWarning() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        stubZone(zone.getId());
        when(fixedEventRepository.saveAndFlush(any(FixedEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Next week's occurrence of today's weekday, so the test does not depend on the time of day it runs.
        LocalDate nextWeek = LocalDate.now(zone).plusWeeks(1);
        int weekday = nextWeek.getDayOfWeek().getValue();
        Instant sessionStart = ZonedDateTime.of(nextWeek, LocalTime.of(9, 30), zone).toInstant();
        when(scheduleApi.committedSessions(eq(USER_ID), any(), any())).thenReturn(List.of(
                new ScheduledSession(100L, 200L, sessionStart, sessionStart.plusSeconds(3600), SessionStatus.CONFIRMED)));

        FixedEventResponse overlapping = fixedEventService.createFixedEvent(USER_ID,
                weeklyRequest(weekday, "09:00", "10:00"));
        assertThat(overlapping.warnings()).containsExactly(FixedEventService.WARNING_SESSION_AFFECTED);

        // Same weekday but after the session ends: no warning (half-open, 10:30 is free).
        FixedEventResponse clear = fixedEventService.createFixedEvent(USER_ID,
                weeklyRequest(weekday, "10:30", "11:30"));
        assertThat(clear.warnings()).isEmpty();
    }

    private void stubZone(String zone) {
        when(userApi.findById(USER_ID)).thenReturn(
                Optional.of(new UserSummary(USER_ID, "fixed@test.com", "User", zone, true)));
    }

    private static FixedEventEntity weekly(int dayOfWeek, String start, String end,
                                           LocalDate effectiveFrom, LocalDate effectiveUntil) {
        FixedEventEntity e = new FixedEventEntity();
        e.setUserId(USER_ID);
        e.setTitle("Class");
        e.setRecurrenceType(RecurrenceType.WEEKLY);
        e.setDayOfWeek((short) dayOfWeek);
        e.setStartTime(LocalTime.parse(start));
        e.setEndTime(LocalTime.parse(end));
        e.setEffectiveFrom(effectiveFrom);
        e.setEffectiveUntil(effectiveUntil);
        return e;
    }

    private static FixedEventRequest weeklyRequest(int dayOfWeek, String start, String end) {
        return new FixedEventRequest("Class", null, RecurrenceType.WEEKLY, dayOfWeek,
                LocalTime.parse(start), LocalTime.parse(end), null, null, null, null);
    }

    private static TimeRange range(String start, String end) {
        return new TimeRange(Instant.parse(start), Instant.parse(end));
    }
}
