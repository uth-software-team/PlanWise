package group16.webapp.modules.schedule.engine;

import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.api.UnscheduledReason;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Deterministic earliest-fit scheduling engine (SC-03, CG-07, BR-08).
 * <ul>
 *   <li>Order: priority desc, deadline asc (none last), remaining duration asc, createdAt asc, taskId asc.</li>
 *   <li>Each task gets at most one continuous session per batch (no splitting).</li>
 *   <li>The session is placed in the earliest free slot that fits the whole remaining duration before the
 *       deadline; otherwise the task is reported as {@code INSUFFICIENT_TIME}.</li>
 *   <li>Intervals are half-open {@code [start, end)}, so adjacent sessions do not overlap.</li>
 *   <li>HC-04: no session crosses a local midnight of the user's timezone; ending exactly at it is allowed.</li>
 * </ul>
 */
@Component
public class SchedulingEngine {

    public record PlannedSlot(Long taskId, Instant startAt, Instant endAt) {}
    public record UnscheduledItem(Long taskId, UnscheduledReason reason, int missingMinutes) {}
    public record EngineResult(List<PlannedSlot> slots, List<UnscheduledItem> unscheduled) {}

    public EngineResult generate(
            List<SchedulableTask> tasks,
            Map<Long, Integer> remainingMinutesByTask,
            List<TimeRange> freeSlots,
            ZoneId userZone,
            Instant now
    ) {
        Comparator<SchedulableTask> order = Comparator
                .comparingInt((SchedulableTask t) -> -priorityWeight(t.priority()))
                .thenComparing(t -> t.deadline() == null ? Instant.MAX : t.deadline())
                .thenComparingInt(t -> remaining(t, remainingMinutesByTask))
                .thenComparing(t -> t.createdAt() == null ? Instant.MAX : t.createdAt())
                .thenComparing(SchedulableTask::taskId, Comparator.nullsLast(Comparator.naturalOrder()));
        List<SchedulableTask> sorted = new ArrayList<>(tasks);
        sorted.sort(order);

        // Free time pool: start -> end, never in the past. Split at local midnights AFTER the final merge (HC-04),
        // so every piece, and therefore every session placed in it, stays within one local day.
        NavigableMap<Instant, Instant> pool = new TreeMap<>();
        for (TimeRange slot : SlotCalculator.mergeOverlapping(freeSlots == null ? List.of() : freeSlots)) {
            Instant effectiveStart = slot.start().isBefore(now) ? now : slot.start();
            if (!effectiveStart.isBefore(slot.end())) continue;
            for (TimeRange piece : SlotCalculator.splitAtLocalMidnights(new TimeRange(effectiveStart, slot.end()), userZone)) {
                pool.put(piece.start(), piece.end());
            }
        }

        List<PlannedSlot> planned = new ArrayList<>();
        List<UnscheduledItem> unscheduled = new ArrayList<>();

        for (SchedulableTask task : sorted) {
            int remaining = remaining(task, remainingMinutesByTask);
            if (remaining <= 0) continue;

            if (task.deadline() != null && !task.deadline().isAfter(now)) {
                unscheduled.add(new UnscheduledItem(task.taskId(), UnscheduledReason.DEADLINE_PASSED, remaining));
                continue;
            }

            Duration needed = Duration.ofMinutes(remaining);
            Instant chosenStart = null;
            for (Map.Entry<Instant, Instant> entry : pool.entrySet()) {
                Instant start = entry.getKey();
                Instant limit = entry.getValue();
                if (task.deadline() != null && task.deadline().isBefore(limit)) {
                    limit = task.deadline();
                }
                if (!start.plus(needed).isAfter(limit)) {
                    chosenStart = start;
                    break;
                }
            }

            if (chosenStart == null) {
                unscheduled.add(new UnscheduledItem(task.taskId(), UnscheduledReason.INSUFFICIENT_TIME, remaining));
                continue;
            }

            Instant sessionEnd = chosenStart.plus(needed);
            Instant slotEnd = pool.remove(chosenStart);
            if (sessionEnd.isBefore(slotEnd)) {
                pool.put(sessionEnd, slotEnd);
            }
            planned.add(new PlannedSlot(task.taskId(), chosenStart, sessionEnd));
        }

        planned.sort(Comparator.comparing(PlannedSlot::startAt));
        return new EngineResult(planned, unscheduled);
    }

    private static int remaining(SchedulableTask task, Map<Long, Integer> remainingMinutesByTask) {
        Integer value = remainingMinutesByTask == null ? null : remainingMinutesByTask.get(task.taskId());
        return value != null ? value : task.estimatedDurationMinutes();
    }

    private static int priorityWeight(TaskPriority priority) {
        if (priority == null) return 0;
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
