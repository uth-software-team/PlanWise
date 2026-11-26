package group16.webapp.modules.schedule.engine;

import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.api.UnscheduledReason;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Greedy Earliest-Deadline-First + Priority scheduling engine (SC-05b). */
@Component
public class SchedulingEngine {

    private static final int MIN_SESSION_MINUTES = 30;
    private static final int MAX_SESSION_MINUTES = 120;

    public record PlannedSlot(Long taskId, Instant startAt, Instant endAt) {}
    public record UnscheduledItem(Long taskId, UnscheduledReason reason, int missingMinutes) {}
    public record EngineResult(List<PlannedSlot> slots, List<UnscheduledItem> unscheduled) {}

    public EngineResult generate(
            List<SchedulableTask> tasks,
            Map<Long, Integer> remainingMinutesByTask,
            List<TimeRange> freeSlots,
            Instant now
    ) {
        List<SchedulableTask> sorted = new ArrayList<>(tasks);
        sorted.sort(Comparator
                .comparing((SchedulableTask t) -> t.deadline() == null ? Instant.MAX : t.deadline())
                .thenComparingInt(t -> -priorityWeight(t.priority()))
                .thenComparing(t -> t.createdAt() == null ? Instant.MAX : t.createdAt()));

        NavigableMap<Instant, Instant> pool = new TreeMap<>();
        for (TimeRange slot : SlotCalculator.mergeOverlapping(freeSlots)) {
            Instant effectiveStart = slot.start().isBefore(now) ? now : slot.start();
            if (effectiveStart.isBefore(slot.end())) {
                pool.put(effectiveStart, slot.end());
            }
        }

        List<PlannedSlot> planned = new ArrayList<>();
        List<UnscheduledItem> unscheduled = new ArrayList<>();

        for (SchedulableTask task : sorted) {
            int remaining = remainingMinutesByTask.getOrDefault(task.taskId(), task.estimatedDurationMinutes());
            if (remaining <= 0) continue;

            if (task.deadline() != null && !task.deadline().isAfter(now)) {
                unscheduled.add(new UnscheduledItem(task.taskId(), UnscheduledReason.DEADLINE_PASSED, remaining));
                continue;
            }

            var snapshot = new ArrayList<>(pool.entrySet());
            for (Map.Entry<Instant, Instant> entry : snapshot) {
                if (remaining <= 0) break;
                Instant s = entry.getKey();
                Instant e = entry.getValue();
                if (!pool.containsKey(s)) continue;

                Instant effectiveEnd = (task.deadline() != null && task.deadline().isBefore(e)) ? task.deadline() : e;
                int availMins = SlotCalculator.minutesBetween(s, effectiveEnd);
                if (availMins < Math.min(MIN_SESSION_MINUTES, remaining)) {
                    continue;
                }

                int allocMins = Math.min(remaining, Math.min(MAX_SESSION_MINUTES, availMins));
                Instant sessionEnd = s.plus(Duration.ofMinutes(allocMins));

                planned.add(new PlannedSlot(task.taskId(), s, sessionEnd));
                remaining -= allocMins;

                pool.remove(s);
                if (sessionEnd.isBefore(e)) {
                    pool.put(sessionEnd, e);
                }
            }

            if (remaining > 0) {
                unscheduled.add(new UnscheduledItem(task.taskId(), UnscheduledReason.INSUFFICIENT_TIME, remaining));
            }
        }

        return new EngineResult(planned, unscheduled);
    }

    private int priorityWeight(TaskPriority priority) {
        if (priority == null) return 0;
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}