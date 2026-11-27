package group16.webapp.modules.availability.service;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.availability.api.CalendarApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.dto.CalendarResponse;
import group16.webapp.modules.availability.dto.CalendarResponse.CalendarFixedEvent;
import group16.webapp.modules.availability.dto.CalendarResponse.CalendarSession;
import group16.webapp.modules.schedule.api.ScheduleApi;
import group16.webapp.modules.schedule.api.ScheduledSession;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskSummary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarService implements CalendarApi {

    private final FixedEventService fixedEventService;
    private final ObjectProvider<ScheduleApi> scheduleApiProvider;
    private final ObjectProvider<TaskApi> taskApiProvider;

    public CalendarService(
            FixedEventService fixedEventService,
            ObjectProvider<ScheduleApi> scheduleApiProvider,
            ObjectProvider<TaskApi> taskApiProvider
    ) {
        this.fixedEventService = fixedEventService;
        this.scheduleApiProvider = scheduleApiProvider;
        this.taskApiProvider = taskApiProvider;
    }

    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(Long userId, Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }

        ZoneId userZone = fixedEventService.resolveUserZone(userId);
        List<CalendarFixedEvent> fixedEvents = fixedEventService.expandCalendarOccurrences(userId, start, end, userZone);

        List<ScheduledSession> committedSessions = fetchCommittedSessions(userId, start, end);
        Map<Long, String> titleByTaskId = fetchTaskTitles(committedSessions);

        List<CalendarSession> sessions = committedSessions.stream()
                .sorted(Comparator.comparing(ScheduledSession::startAt)
                        .thenComparing(ScheduledSession::endAt)
                        .thenComparing(ScheduledSession::sessionId))
                .map(s -> new CalendarSession(
                        s.sessionId(),
                        s.taskId(),
                        titleByTaskId.getOrDefault(s.taskId(), "Task #" + s.taskId()),
                        s.startAt(),
                        s.endAt(),
                        s.status().name()
                ))
                .toList();

        return new CalendarResponse(
                start,
                end,
                userZone.getId(),
                fixedEvents,
                sessions
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> busyIntervals(Long userId, Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            return List.of();
        }

        List<TimeRange> combined = new ArrayList<>(fixedEventService.expandMergedOccurrences(userId, start, end));

        for (ScheduledSession session : fetchCommittedSessions(userId, start, end)) {
            Instant clippedStart = session.startAt().isAfter(start) ? session.startAt() : start;
            Instant clippedEnd = session.endAt().isBefore(end) ? session.endAt() : end;
            if (clippedStart.isBefore(clippedEnd)) {
                combined.add(new TimeRange(clippedStart, clippedEnd));
            }
        }

        return AvailabilityService.mergeRanges(combined);
    }

    private List<ScheduledSession> fetchCommittedSessions(Long userId, Instant start, Instant end) {
        ScheduleApi scheduleApi = scheduleApiProvider.getIfAvailable();
        if (scheduleApi == null) {
            return List.of();
        }
        List<ScheduledSession> sessions = scheduleApi.committedSessions(userId, start, end);
        return sessions != null ? sessions : List.of();
    }

    private Map<Long, String> fetchTaskTitles(List<ScheduledSession> sessions) {
        if (sessions.isEmpty()) {
            return Map.of();
        }
        TaskApi taskApi = taskApiProvider.getIfAvailable();
        if (taskApi == null) {
            return Map.of();
        }
        Set<Long> taskIds = sessions.stream()
                .map(ScheduledSession::taskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return taskApi.summaries(taskIds).stream()
                .collect(Collectors.toMap(TaskSummary::taskId, TaskSummary::title, (a, b) -> a));
    }
}