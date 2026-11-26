package group16.webapp.modules.schedule.engine;

import group16.webapp.common.dto.ConflictErrorDto;
import group16.webapp.common.exception.ConflictCode;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.schedule.domain.ScheduleSession;
import group16.webapp.modules.task.api.TaskSummary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/** Validates hard constraints HC-01..HC-08 for batch sessions (SC-04 / SC-07). */
@Component
public class ConstraintValidator {

    public List<ConflictErrorDto> validateBatch(
            List<ScheduleSession> batchSessions,
            List<TimeRange> availableIntervals,
            List<TimeRange> fixedEvents,
            List<TimeRange> existingCommittedSessions,
            Map<Long, TaskSummary> taskSummaries,
            Map<Long, Integer> remainingMinutesByTask,
            Set<Long> schedulableTaskIds,
            Instant now
    ) {
        List<ConflictErrorDto> conflicts = new ArrayList<>();
        Map<Long, Integer> plannedMinutesByTask = new HashMap<>();

        for (int i = 0; i < batchSessions.size(); i++) {
            ScheduleSession s = batchSessions.get(i);
            TimeRange range = new TimeRange(s.getStartAt(), s.getEndAt());
            TaskSummary task = taskSummaries.get(s.getTaskId());

            // HC-08: task must be schedulable (personal TODO/IN_PROGRESS or ACCEPTED group assignment)
            if (task == null || !schedulableTaskIds.contains(s.getTaskId())) {
                conflicts.add(new ConflictErrorDto(s.getId(), ConflictCode.TASK_NOT_SCHEDULABLE,
                        "Task is no longer eligible for scheduling"));
            }

            // HC-05: cannot schedule in the past
            if (s.getStartAt().isBefore(now)) {
                conflicts.add(new ConflictErrorDto(s.getId(), ConflictCode.PAST_TIME,
                        "Session starts in the past"));
            }

            // HC-06: ends after task deadline
            if (task != null && task.deadline() != null && s.getEndAt().isAfter(task.deadline())) {
                conflicts.add(new ConflictErrorDto(s.getId(), ConflictCode.DEADLINE_EXCEEDED,
                        "Session ends after task deadline"));
            }

            // HC-03: must fall completely inside active availability rules
            if (!SlotCalculator.isContainedWithin(range, availableIntervals)) {
                conflicts.add(new ConflictErrorDto(s.getId(), ConflictCode.OUTSIDE_AVAILABILITY,
                        "Session is outside active availability hours"));
            }

            // HC-02: must not overlap fixed events
            if (SlotCalculator.overlapsAny(range, fixedEvents)) {
                conflicts.add(new ConflictErrorDto(s.getId(), ConflictCode.FIXED_EVENT_OVERLAP,
                        "Session overlaps a fixed event"));
            }

            // HC-01: must not overlap existing committed sessions or sibling sessions in this batch
            boolean overlapsSibling = false;
            for (int j = 0; j < batchSessions.size(); j++) {
                if (i == j) continue;
                ScheduleSession other = batchSessions.get(j);
                if (s.getStartAt().isBefore(other.getEndAt()) && other.getStartAt().isBefore(s.getEndAt())) {
                    overlapsSibling = true;
                    break;
                }
            }
            if (overlapsSibling || SlotCalculator.overlapsAny(range, existingCommittedSessions)) {
                conflicts.add(new ConflictErrorDto(s.getId(), ConflictCode.SESSION_OVERLAP,
                        "Session overlaps another scheduled session"));
            }

            // HC-07: cumulative planned duration must not exceed remaining duration
            int mins = SlotCalculator.minutesBetween(s.getStartAt(), s.getEndAt());
            int totalPlanned = plannedMinutesByTask.merge(s.getTaskId(), mins, Integer::sum);
            Integer maxAllowed = remainingMinutesByTask.get(s.getTaskId());
            if (maxAllowed != null && totalPlanned > maxAllowed) {
                conflicts.add(new ConflictErrorDto(s.getId(), ConflictCode.DURATION_EXCEEDED,
                        "Planned duration exceeds remaining task duration"));
            }
        }

        return conflicts;
    }
}
