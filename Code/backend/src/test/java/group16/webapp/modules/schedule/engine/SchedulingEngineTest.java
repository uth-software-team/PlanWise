package group16.webapp.modules.schedule.engine;

import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.api.UnscheduledReason;
import group16.webapp.modules.schedule.engine.SchedulingEngine.EngineResult;
import group16.webapp.modules.schedule.engine.SchedulingEngine.PlannedSlot;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskPriority;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingEngineTest {

    private static final Instant NOW = t("2026-11-30T08:00");
    private static final Instant CREATED = t("2026-11-01T00:00");
    private static final Instant FAR_DEADLINE = t("2026-12-10T00:00");

    /** Existing cases use UTC; all their slots stay within one UTC day, so HC-04 splitting does not change them. */
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SchedulingEngine engine = new SchedulingEngine();

    private static Instant t(String isoMinute) {
        return Instant.parse(isoMinute + ":00Z");
    }

    private static TimeRange r(String start, String end) {
        return new TimeRange(t("2026-11-30T" + start), t("2026-11-30T" + end));
    }

    private static SchedulableTask task(long id, TaskPriority p, Instant deadline, int minutes) {
        return new SchedulableTask(id, deadline, p, minutes, CREATED);
    }

    private EngineResult run(List<SchedulableTask> tasks, List<TimeRange> free) {
        return engine.generate(tasks, Map.of(), free, ZONE, NOW);
    }

    private static List<Long> order(EngineResult result) {
        return result.slots().stream().map(PlannedSlot::taskId).toList();
    }

    @Test
    void higherPriorityIsPlacedFirstEvenWithLaterDeadline() {
        var urgentLate = task(1L, TaskPriority.URGENT, t("2026-12-05T10:00"), 60);
        var lowEarly = task(2L, TaskPriority.LOW, t("2026-12-01T10:00"), 60);

        var result = run(List.of(lowEarly, urgentLate), List.of(r("09:00", "11:00")));

        assertEquals(List.of(1L, 2L), order(result));
        assertEquals(r("09:00", "10:00").start(), result.slots().get(0).startAt());
        assertTrue(result.unscheduled().isEmpty());
    }

    @Test
    void samePriorityEarlierDeadlineFirst_noDeadlineLast() {
        var noDeadline = task(1L, TaskPriority.HIGH, null, 30);
        var later = task(2L, TaskPriority.HIGH, t("2026-12-05T00:00"), 30);
        var earlier = task(3L, TaskPriority.HIGH, t("2026-12-02T00:00"), 30);

        var result = run(List.of(noDeadline, later, earlier), List.of(r("09:00", "12:00")));

        assertEquals(List.of(3L, 2L, 1L), order(result));
    }

    @Test
    void tieOnPriorityAndDeadlineBrokenByShorterRemainingThenCreatedAtThenTaskId() {
        var longer = new SchedulableTask(1L, FAR_DEADLINE, TaskPriority.MEDIUM, 90, CREATED);
        var shorterNewer = new SchedulableTask(2L, FAR_DEADLINE, TaskPriority.MEDIUM, 30, CREATED.plusSeconds(60));
        var shorterOlderHighId = new SchedulableTask(9L, FAR_DEADLINE, TaskPriority.MEDIUM, 30, CREATED);
        var shorterOlderLowId = new SchedulableTask(4L, FAR_DEADLINE, TaskPriority.MEDIUM, 30, CREATED);

        var result = run(List.of(longer, shorterNewer, shorterOlderHighId, shorterOlderLowId), List.of(r("09:00", "13:00")));

        assertEquals(List.of(4L, 9L, 2L, 1L), order(result));
    }

    @Test
    void remainingMinutesOverrideEstimateForOrderingAndLength() {
        var a = task(1L, TaskPriority.MEDIUM, FAR_DEADLINE, 120);
        var b = task(2L, TaskPriority.MEDIUM, FAR_DEADLINE, 60);

        // Task 1 already has 90 confirmed minutes -> only 30 remaining, so it goes first and lasts 30 min.
        var result = engine.generate(List.of(a, b), Map.of(1L, 30, 2L, 60), List.of(r("09:00", "12:00")), ZONE, NOW);

        assertEquals(List.of(1L, 2L), order(result));
        assertEquals(r("09:00", "09:30"), new TimeRange(result.slots().get(0).startAt(), result.slots().get(0).endAt()));
    }

    @Test
    void taskWithNoRemainingTimeIsSkipped() {
        var done = task(1L, TaskPriority.HIGH, FAR_DEADLINE, 60);
        var result = engine.generate(List.of(done), Map.of(1L, 0), List.of(r("09:00", "12:00")), ZONE, NOW);

        assertTrue(result.slots().isEmpty());
        assertTrue(result.unscheduled().isEmpty());
    }

    @Test
    void oneContinuousSessionPerTask_noSplittingAcrossSlots() {
        // 90 minutes needed; two 60-minute gaps are not enough without splitting (BR-08).
        var t1 = task(1L, TaskPriority.HIGH, FAR_DEADLINE, 90);

        var result = run(List.of(t1), List.of(r("09:00", "10:00"), r("11:00", "12:00")));

        assertTrue(result.slots().isEmpty());
        assertEquals(1, result.unscheduled().size());
        assertEquals(UnscheduledReason.INSUFFICIENT_TIME, result.unscheduled().get(0).reason());
        assertEquals(90, result.unscheduled().get(0).missingMinutes());
    }

    @Test
    void longTaskGetsOneSessionOfFullLength() {
        var t1 = task(1L, TaskPriority.HIGH, FAR_DEADLINE, 180);

        var result = run(List.of(t1), List.of(r("09:00", "13:00")));

        assertEquals(1, result.slots().size());
        assertEquals(r("09:00", "12:00").end(), result.slots().get(0).endAt());
    }

    @Test
    void earliestFitSkipsTooSmallSlotAndUsesLaterOne() {
        var t1 = task(1L, TaskPriority.HIGH, FAR_DEADLINE, 60);

        var result = run(List.of(t1), List.of(r("09:00", "09:30"), r("10:00", "11:00")));

        assertEquals(r("10:00", "11:00").start(), result.slots().get(0).startAt());
    }

    @Test
    void lowerPriorityTaskFillsGapLeftBySmallerSlot() {
        var big = task(1L, TaskPriority.URGENT, FAR_DEADLINE, 60);
        var small = task(2L, TaskPriority.LOW, FAR_DEADLINE, 30);

        var result = run(List.of(big, small), List.of(r("09:00", "09:30"), r("10:00", "11:00")));

        assertEquals(2, result.slots().size());
        // Output is sorted by start time: the small task got the earlier 30-minute gap.
        assertEquals(2L, result.slots().get(0).taskId());
        assertEquals(r("09:00", "09:30").start(), result.slots().get(0).startAt());
        assertEquals(1L, result.slots().get(1).taskId());
    }

    @Test
    void sessionsAreBackToBackWithoutOverlap_halfOpen() {
        var a = task(1L, TaskPriority.HIGH, FAR_DEADLINE, 60);
        var b = task(2L, TaskPriority.MEDIUM, FAR_DEADLINE, 60);

        var result = run(List.of(a, b), List.of(r("09:00", "11:00")));

        assertEquals(result.slots().get(0).endAt(), result.slots().get(1).startAt());
    }

    @Test
    void sessionMustEndByDeadline() {
        var due = task(1L, TaskPriority.HIGH, t("2026-11-30T09:30"), 60);

        var result = run(List.of(due), List.of(r("09:00", "12:00")));

        assertTrue(result.slots().isEmpty());
        assertEquals(UnscheduledReason.INSUFFICIENT_TIME, result.unscheduled().get(0).reason());
    }

    @Test
    void sessionEndingExactlyAtDeadlineIsAllowed() {
        var due = task(1L, TaskPriority.HIGH, t("2026-11-30T10:00"), 60);

        var result = run(List.of(due), List.of(r("09:00", "12:00")));

        assertEquals(t("2026-11-30T10:00"), result.slots().get(0).endAt());
    }

    @Test
    void deadlineAtOrBeforeNowIsDeadlinePassed() {
        var past = task(1L, TaskPriority.HIGH, t("2026-11-29T08:00"), 30);
        var exactlyNow = task(2L, TaskPriority.HIGH, NOW, 30);

        var result = run(List.of(past, exactlyNow), List.of(r("09:00", "12:00")));

        assertTrue(result.slots().isEmpty());
        assertTrue(result.unscheduled().stream().allMatch(u -> u.reason() == UnscheduledReason.DEADLINE_PASSED));
        assertEquals(2, result.unscheduled().size());
    }

    @Test
    void neverSchedulesInThePast() {
        var t1 = task(1L, TaskPriority.HIGH, FAR_DEADLINE, 60);

        // Free slot started before now (08:00); the session must start at now.
        var result = run(List.of(t1), List.of(r("07:00", "10:00")));

        assertEquals(NOW, result.slots().get(0).startAt());
    }

    @Test
    void overloadReportsEveryTaskThatDoesNotFit() {
        var a = task(1L, TaskPriority.URGENT, FAR_DEADLINE, 60);
        var b = task(2L, TaskPriority.HIGH, FAR_DEADLINE, 60);
        var c = task(3L, TaskPriority.LOW, FAR_DEADLINE, 60);

        var result = run(List.of(a, b, c), List.of(r("09:00", "10:00")));

        assertEquals(List.of(1L), order(result));
        assertEquals(List.of(2L, 3L), result.unscheduled().stream().map(SchedulingEngine.UnscheduledItem::taskId).toList());
    }

    @Test
    void noFreeTimeMakesAllTasksInsufficient() {
        var result = run(List.of(task(1L, TaskPriority.HIGH, FAR_DEADLINE, 30)), List.of());

        assertTrue(result.slots().isEmpty());
        assertEquals(UnscheduledReason.INSUFFICIENT_TIME, result.unscheduled().get(0).reason());
    }

    // HC-04 in Asia/Ho_Chi_Minh (+07): 15:00Z = 22:00, 16:00Z = 23:00, 17:00Z = 00:00, 19:00Z = 02:00 local.

    private EngineResult runIn(ZoneId zone, SchedulableTask task, TimeRange free) {
        return engine.generate(List.of(task), Map.of(), List.of(free), zone, NOW);
    }

    @Test
    void hc04_sessionEndsExactlyAtLocalMidnight() {
        var result = runIn(VN, task(1L, TaskPriority.HIGH, FAR_DEADLINE, 120), r("15:00", "19:00"));

        assertEquals(List.of(new PlannedSlot(1L, t("2026-11-30T15:00"), t("2026-11-30T17:00"))), result.slots());
    }

    @Test
    void hc04_taskThatWouldCrossLocalMidnightIsInsufficient() {
        var result = runIn(VN, task(1L, TaskPriority.HIGH, FAR_DEADLINE, 180), r("15:00", "19:00"));

        assertTrue(result.slots().isEmpty());
        assertEquals(UnscheduledReason.INSUFFICIENT_TIME, result.unscheduled().get(0).reason());
    }

    @Test
    void hc04_taskMovesToNextLocalDayWhenBeforeMidnightIsTooShort() {
        // 23:00–02:00 local: 60 min before midnight, 120 min after; a 90-min task starts at 00:00.
        var result = runIn(VN, task(1L, TaskPriority.HIGH, FAR_DEADLINE, 90), r("16:00", "19:00"));

        assertEquals(List.of(new PlannedSlot(1L, t("2026-11-30T17:00"), t("2026-11-30T18:30"))), result.slots());
    }

    @Test
    void hc04_sameInstantsGiveDifferentResultsPerZone() {
        var task = task(1L, TaskPriority.HIGH, FAR_DEADLINE, 180);

        assertEquals(1, runIn(ZoneOffset.UTC, task, r("16:00", "19:00")).slots().size());
        assertTrue(runIn(VN, task, r("16:00", "19:00")).slots().isEmpty());
    }

    @Test
    void hc04_adjacentFreeSlotsAcrossMidnightAreNotJoined() {
        // Two adjacent inputs meeting at local midnight are merged first, then split again at midnight.
        var result = engine.generate(List.of(task(1L, TaskPriority.HIGH, FAR_DEADLINE, 180)), Map.of(),
                List.of(r("15:00", "17:00"), r("17:00", "19:00")), VN, NOW);

        assertTrue(result.slots().isEmpty());
    }

    @Test
    void deterministic_sameInputInAnyOrderGivesSameOutput() {
        List<SchedulableTask> tasks = new ArrayList<>(List.of(
                task(1L, TaskPriority.MEDIUM, FAR_DEADLINE, 45),
                task(2L, TaskPriority.URGENT, t("2026-12-01T00:00"), 30),
                task(3L, TaskPriority.MEDIUM, FAR_DEADLINE, 45),
                task(4L, TaskPriority.LOW, null, 60),
                task(5L, TaskPriority.HIGH, t("2026-11-30T09:00"), 90)));
        var free = List.of(r("09:00", "10:30"), r("13:00", "15:00"));

        var first = run(tasks, free);
        Collections.reverse(tasks);
        var second = run(tasks, free);

        assertEquals(first, second);
    }
}
