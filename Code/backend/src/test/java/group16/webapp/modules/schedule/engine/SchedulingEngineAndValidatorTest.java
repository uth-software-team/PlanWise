package group16.webapp.modules.schedule.engine;

import group16.webapp.common.dto.ConflictErrorDto;
import group16.webapp.common.exception.ConflictCode;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.api.UnscheduledReason;
import group16.webapp.modules.schedule.domain.ScheduleSession;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.api.TaskSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingEngineAndValidatorTest {

    private final SchedulingEngine engine = new SchedulingEngine();
    private final ConstraintValidator validator = new ConstraintValidator();

    @Test
    void generate_prioritizesEarliestDeadlineThenPriority() {
        Instant now = Instant.parse("2026-11-30T08:00:00Z");
        Instant slotStart = Instant.parse("2026-11-30T09:00:00Z");
        Instant slotEnd = Instant.parse("2026-11-30T11:00:00Z");

        var laterTask = new SchedulableTask(1L, Instant.parse("2026-12-05T10:00:00Z"), TaskPriority.URGENT, 60, now);
        var earlierTask = new SchedulableTask(2L, Instant.parse("2026-12-01T10:00:00Z"), TaskPriority.LOW, 60, now);

        var result = engine.generate(
                List.of(laterTask, earlierTask),
                Map.of(1L, 60, 2L, 60),
                List.of(new TimeRange(slotStart, slotEnd)),
                now
        );

        assertEquals(2, result.slots().size());
        assertEquals(2L, result.slots().get(0).taskId());
        assertEquals(1L, result.slots().get(1).taskId());
        assertTrue(result.unscheduled().isEmpty());
    }

    @Test
    void generate_marksPastDeadlineAndInsufficientTime() {
        Instant now = Instant.parse("2026-11-30T08:00:00Z");
        var expired = new SchedulableTask(1L, Instant.parse("2026-11-29T08:00:00Z"), TaskPriority.HIGH, 60, now);
        var tooLong = new SchedulableTask(2L, Instant.parse("2026-12-02T08:00:00Z"), TaskPriority.MEDIUM, 180, now);

        var result = engine.generate(
                List.of(expired, tooLong),
                Map.of(1L, 60, 2L, 180),
                List.of(new TimeRange(Instant.parse("2026-11-30T09:00:00Z"), Instant.parse("2026-11-30T10:00:00Z"))),
                now
        );

        assertEquals(1, result.slots().size());
        assertEquals(2, result.unscheduled().size());
        assertEquals(UnscheduledReason.DEADLINE_PASSED, result.unscheduled().get(0).reason());
        assertEquals(UnscheduledReason.INSUFFICIENT_TIME, result.unscheduled().get(1).reason());
        assertEquals(120, result.unscheduled().get(1).missingMinutes());
    }

    @Test
    void validateBatch_detectsHardConstraintViolations() {
        Instant now = Instant.parse("2026-11-30T08:00:00Z");
        ScheduleSession s1 = new ScheduleSession();
        s1.setTaskId(10L);
        s1.setStartAt(Instant.parse("2026-11-30T07:00:00Z")); // PAST_TIME & OUTSIDE_AVAILABILITY
        s1.setEndAt(Instant.parse("2026-11-30T08:30:00Z"));

        var avail = List.of(new TimeRange(Instant.parse("2026-11-30T09:00:00Z"), Instant.parse("2026-11-30T12:00:00Z")));
        var fixed = List.of(new TimeRange(Instant.parse("2026-11-30T08:00:00Z"), Instant.parse("2026-11-30T08:30:00Z")));
        var task = new TaskSummary(10L, null, "Study", Instant.parse("2026-11-30T08:15:00Z"), TaskPriority.HIGH, TaskStatus.TODO);

        List<ConflictErrorDto> conflicts = validator.validateBatch(
                List.of(s1), avail, fixed, List.of(),
                Map.of(10L, task), Map.of(10L, 60), Set.of(10L), now
        );

        var codes = conflicts.stream().map(ConflictErrorDto::code).toList();
        assertTrue(codes.contains(ConflictCode.PAST_TIME));
        assertTrue(codes.contains(ConflictCode.OUTSIDE_AVAILABILITY));
        assertTrue(codes.contains(ConflictCode.FIXED_EVENT_OVERLAP));
        assertTrue(codes.contains(ConflictCode.DEADLINE_EXCEEDED));
        assertTrue(codes.contains(ConflictCode.DURATION_EXCEEDED));
    }
}