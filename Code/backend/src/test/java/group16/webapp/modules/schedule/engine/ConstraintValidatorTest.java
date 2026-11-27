package group16.webapp.modules.schedule.engine;

import group16.webapp.common.dto.ConflictErrorDto;
import group16.webapp.common.exception.ConflictCode;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.domain.ScheduleSession;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.api.TaskSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintValidatorTest {

    private static final Instant NOW = t("07:00");
    private static final long TASK = 10L;

    private final ConstraintValidator validator = new ConstraintValidator();

    private static Instant t(String hhmm) {
        return Instant.parse("2026-11-30T" + hhmm + ":00Z");
    }

    private static TimeRange r(String start, String end) {
        return new TimeRange(t(start), t(end));
    }

    private static ScheduleSession session(long taskId, String start, String end) {
        ScheduleSession s = new ScheduleSession();
        s.setTaskId(taskId);
        s.setStartAt(t(start));
        s.setEndAt(t(end));
        return s;
    }

    private static TaskSummary summary(long taskId, Instant deadline) {
        return new TaskSummary(taskId, null, "Study", deadline, TaskPriority.HIGH, TaskStatus.TODO);
    }

    /** Valid baseline: availability 09–12, no fixed events, no committed sessions, deadline far away, 120 min left. */
    private List<ConflictCode> validate(List<ScheduleSession> sessions, List<TimeRange> fixed, List<TimeRange> committed,
                                        Instant deadline, Map<Long, Integer> remaining, Set<Long> schedulable) {
        Map<Long, TaskSummary> summaries = new java.util.HashMap<>();
        sessions.forEach(s -> summaries.put(s.getTaskId(), summary(s.getTaskId(), deadline)));
        return validator.validateBatch(sessions, List.of(r("09:00", "12:00")), fixed, committed,
                        summaries, remaining, schedulable, NOW)
                .stream().map(ConflictErrorDto::code).toList();
    }

    private List<ConflictCode> validateOne(ScheduleSession s) {
        return validate(List.of(s), List.of(), List.of(), t("23:00"), Map.of(TASK, 120), Set.of(TASK));
    }

    @Test
    void validSessionHasNoConflicts() {
        assertTrue(validateOne(session(TASK, "09:00", "10:00")).isEmpty());
    }

    @Test
    void hc01_overlapWithCommittedSession() {
        var codes = validate(List.of(session(TASK, "09:00", "10:00")), List.of(), List.of(r("09:30", "10:30")),
                t("23:00"), Map.of(TASK, 120), Set.of(TASK));
        assertEquals(List.of(ConflictCode.SESSION_OVERLAP), codes);
    }

    @Test
    void hc01_overlapBetweenSessionsInSameBatch() {
        var codes = validate(List.of(session(TASK, "09:00", "10:00"), session(11L, "09:30", "10:30")),
                List.of(), List.of(), t("23:00"), Map.of(TASK, 120, 11L, 120), Set.of(TASK, 11L));
        assertEquals(List.of(ConflictCode.SESSION_OVERLAP, ConflictCode.SESSION_OVERLAP), codes);
    }

    @Test
    void hc01_adjacentSessionsDoNotOverlap() {
        var codes = validate(List.of(session(TASK, "09:00", "10:00"), session(11L, "10:00", "11:00")),
                List.of(), List.of(r("11:00", "12:00")), t("23:00"), Map.of(TASK, 120, 11L, 120), Set.of(TASK, 11L));
        assertTrue(codes.isEmpty());
    }

    @Test
    void hc02_overlapWithFixedEvent() {
        var codes = validate(List.of(session(TASK, "09:00", "10:00")), List.of(r("09:45", "10:15")), List.of(),
                t("23:00"), Map.of(TASK, 120), Set.of(TASK));
        assertEquals(List.of(ConflictCode.FIXED_EVENT_OVERLAP), codes);
    }

    @Test
    void hc03_outsideAvailability() {
        assertEquals(List.of(ConflictCode.OUTSIDE_AVAILABILITY), validateOne(session(TASK, "11:30", "12:30")));
    }

    @Test
    void hc05_startsInThePast() {
        var codes = validator.validateBatch(List.of(session(TASK, "09:00", "10:00")), List.of(r("09:00", "12:00")),
                List.of(), List.of(), Map.of(TASK, summary(TASK, t("23:00"))), Map.of(TASK, 120), Set.of(TASK),
                t("09:01")).stream().map(ConflictErrorDto::code).toList();
        assertEquals(List.of(ConflictCode.PAST_TIME), codes);
    }

    @Test
    void hc06_endsAfterDeadline_butEndingExactlyAtDeadlineIsAllowed() {
        assertEquals(List.of(ConflictCode.DEADLINE_EXCEEDED),
                validate(List.of(session(TASK, "09:00", "10:00")), List.of(), List.of(), t("09:59"),
                        Map.of(TASK, 120), Set.of(TASK)));
        assertTrue(validate(List.of(session(TASK, "09:00", "10:00")), List.of(), List.of(), t("10:00"),
                Map.of(TASK, 120), Set.of(TASK)).isEmpty());
    }

    @Test
    void hc07_plannedDurationExceedsRemaining() {
        assertEquals(List.of(ConflictCode.DURATION_EXCEEDED),
                validate(List.of(session(TASK, "09:00", "10:30")), List.of(), List.of(), t("23:00"),
                        Map.of(TASK, 60), Set.of(TASK)));
    }

    @Test
    void hc08_taskNoLongerSchedulable() {
        assertEquals(List.of(ConflictCode.TASK_NOT_SCHEDULABLE),
                validate(List.of(session(TASK, "09:00", "10:00")), List.of(), List.of(), t("23:00"),
                        Map.of(TASK, 120), Set.of()));
    }

    @Test
    void hc08_missingTaskSummaryIsNotSchedulable() {
        var codes = validator.validateBatch(List.of(session(TASK, "09:00", "10:00")), List.of(r("09:00", "12:00")),
                List.of(), List.of(), Map.of(), Map.of(TASK, 120), Set.of(TASK), NOW)
                .stream().map(ConflictErrorDto::code).toList();
        assertEquals(List.of(ConflictCode.TASK_NOT_SCHEDULABLE), codes);
    }

    @Test
    void multipleViolationsAreAllReported() {
        var codes = validator.validateBatch(List.of(session(TASK, "06:00", "08:30")), List.of(r("09:00", "12:00")),
                List.of(r("08:00", "08:30")), List.of(), Map.of(TASK, summary(TASK, t("08:15"))), Map.of(TASK, 60),
                Set.of(TASK), NOW).stream().map(ConflictErrorDto::code).toList();
        assertTrue(codes.containsAll(List.of(ConflictCode.PAST_TIME, ConflictCode.DEADLINE_EXCEEDED,
                ConflictCode.OUTSIDE_AVAILABILITY, ConflictCode.FIXED_EVENT_OVERLAP, ConflictCode.DURATION_EXCEEDED)));
    }
}
