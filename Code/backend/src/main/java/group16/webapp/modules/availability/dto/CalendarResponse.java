package group16.webapp.modules.availability.dto;

import group16.webapp.modules.availability.api.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * The caller's calendar for {@code [start, end)} (design doc CalendarDTO, UC-10): fixed-event occurrences and
 * CONFIRMED/COMPLETED sessions (PROPOSED sessions are shown by the proposal endpoints). Instants are UTC;
 * {@code timezone} is the user's IANA zone for display.
 */
public record CalendarResponse(
        Instant start,
        Instant end,
        String timezone,
        List<CalendarFixedEvent> fixedEvents,
        List<CalendarSession> sessions
) {

    /** One occurrence of a fixed event. */
    public record CalendarFixedEvent(
            Long fixedEventId,
            String title,
            RecurrenceType recurrenceType,
            Instant startAt,
            Instant endAt
    ) {
    }

    /** One scheduled session. status is a string until the schedule module (SC-01) publishes SessionStatus. */
    public record CalendarSession(
            Long sessionId,
            Long taskId,
            String taskTitle,
            Instant startAt,
            Instant endAt,
            @Schema(allowableValues = {"CONFIRMED", "COMPLETED"}) String status
    ) {
    }
}
