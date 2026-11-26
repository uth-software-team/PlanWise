package group16.webapp.modules.schedule.engine;

import group16.webapp.modules.availability.api.TimeRange;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Computes free time slots by subtracting busy intervals from available intervals (half-open [start, end)). */
public final class SlotCalculator {

    private SlotCalculator() {}

    public static List<TimeRange> subtractBusy(List<TimeRange> available, List<TimeRange> busy) {
        if (available == null || available.isEmpty()) return List.of();
        List<TimeRange> normalizedAvail = mergeOverlapping(available);
        List<TimeRange> normalizedBusy = busy == null ? List.of() : mergeOverlapping(busy);

        List<TimeRange> free = new ArrayList<>();
        for (TimeRange av : normalizedAvail) {
            Instant cursor = av.start();
            for (TimeRange b : normalizedBusy) {
                if (!b.end().isAfter(cursor)) continue;
                if (!b.start().isBefore(av.end())) break;
                if (b.start().isAfter(cursor)) {
                    free.add(new TimeRange(cursor, b.start()));
                }
                if (b.end().isAfter(cursor)) {
                    cursor = b.end();
                }
            }
            if (cursor.isBefore(av.end())) {
                free.add(new TimeRange(cursor, av.end()));
            }
        }
        return free;
    }

    public static List<TimeRange> mergeOverlapping(List<TimeRange> intervals) {
        List<TimeRange> valid = intervals.stream()
                .filter(r -> r != null && r.start() != null && r.end() != null && r.start().isBefore(r.end()))
                .sorted(Comparator.comparing(TimeRange::start).thenComparing(TimeRange::end))
                .toList();
        if (valid.isEmpty()) return List.of();

        List<TimeRange> merged = new ArrayList<>();
        TimeRange current = valid.get(0);
        for (int i = 1; i < valid.size(); i++) {
            TimeRange next = valid.get(i);
            if (!next.start().isAfter(current.end())) {
                Instant maxEnd = next.end().isAfter(current.end()) ? next.end() : current.end();
                current = new TimeRange(current.start(), maxEnd);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    public static boolean isContainedWithin(TimeRange target, List<TimeRange> containers) {
        for (TimeRange c : mergeOverlapping(containers)) {
            if (!target.start().isBefore(c.start()) && !target.end().isAfter(c.end())) {
                return true;
            }
        }
        return false;
    }

    public static boolean overlapsAny(TimeRange target, List<TimeRange> others) {
        for (TimeRange o : others) {
            if (target.start().isBefore(o.end()) && o.start().isBefore(target.end())) {
                return true;
            }
        }
        return false;
    }

    public static int minutesBetween(Instant start, Instant end) {
        return (int) Duration.between(start, end).toMinutes();
    }
}
