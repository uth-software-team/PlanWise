package group16.webapp.modules.availability.service;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.RecurrenceType;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.dto.CalendarResponse.CalendarFixedEvent;
import group16.webapp.modules.availability.dto.FixedEventRequest;
import group16.webapp.modules.availability.dto.FixedEventResponse;
import group16.webapp.modules.availability.entity.FixedEventEntity;
import group16.webapp.modules.availability.repository.FixedEventRepository;
import group16.webapp.modules.schedule.api.ScheduleApi;
import group16.webapp.modules.schedule.api.ScheduledSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FixedEventService {

    public static final String WARNING_SESSION_AFFECTED = "SESSION_AFFECTED";

    private final FixedEventRepository fixedEventRepository;
    private final ObjectProvider<UserApi> userApiProvider;
    private final ObjectProvider<ScheduleApi> scheduleApiProvider;

    public FixedEventService(
            FixedEventRepository fixedEventRepository,
            ObjectProvider<UserApi> userApiProvider,
            ObjectProvider<ScheduleApi> scheduleApiProvider
    ) {
        this.fixedEventRepository = fixedEventRepository;
        this.userApiProvider = userApiProvider;
        this.scheduleApiProvider = scheduleApiProvider;
    }

    @Transactional(readOnly = true)
    public List<FixedEventResponse> listFixedEvents(Long userId) {
        return fixedEventRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .map(e -> toResponse(e, List.of()))
                .toList();
    }

    @Transactional
    public FixedEventResponse createFixedEvent(Long userId, FixedEventRequest request) {
        validateRequest(request);

        FixedEventEntity entity = new FixedEventEntity();
        entity.setUserId(userId);
        applyRequest(entity, request);

        FixedEventEntity saved = fixedEventRepository.saveAndFlush(entity);
        List<String> warnings = detectSessionAffectedWarnings(userId, saved);
        return toResponse(saved, warnings);
    }

    @Transactional
    public FixedEventResponse updateFixedEvent(Long userId, Long eventId, FixedEventRequest request) {
        validateRequest(request);

        FixedEventEntity entity = fixedEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        applyRequest(entity, request);

        FixedEventEntity saved = fixedEventRepository.saveAndFlush(entity);
        List<String> warnings = detectSessionAffectedWarnings(userId, saved);
        return toResponse(saved, warnings);
    }

    @Transactional
    public void deleteFixedEvent(Long userId, Long eventId) {
        FixedEventEntity entity = fixedEventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        fixedEventRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<TimeRange> expandMergedOccurrences(Long userId, Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            return List.of();
        }
        List<FixedEventEntity> events = fixedEventRepository.findByUserId(userId);
        if (events.isEmpty()) {
            return List.of();
        }
        ZoneId userZone = resolveUserZone(userId);
        List<TimeRange> raw = new ArrayList<>();
        for (FixedEventEntity event : events) {
            for (TimeRange occ : expandSingleEvent(event, userZone, start, end)) {
                Instant clippedStart = occ.start().isAfter(start) ? occ.start() : start;
                Instant clippedEnd = occ.end().isBefore(end) ? occ.end() : end;
                if (clippedStart.isBefore(clippedEnd)) {
                    raw.add(new TimeRange(clippedStart, clippedEnd));
                }
            }
        }
        return AvailabilityService.mergeRanges(raw);
    }

    @Transactional(readOnly = true)
    public List<CalendarFixedEvent> expandCalendarOccurrences(Long userId, Instant start, Instant end, ZoneId userZone) {
        List<FixedEventEntity> events = fixedEventRepository.findByUserId(userId);
        if (events.isEmpty()) {
            return List.of();
        }
        List<CalendarFixedEvent> result = new ArrayList<>();
        for (FixedEventEntity event : events) {
            for (TimeRange occ : expandSingleEvent(event, userZone, start, end)) {
                result.add(new CalendarFixedEvent(
                        event.getId(),
                        event.getTitle(),
                        event.getRecurrenceType(),
                        occ.start(),
                        occ.end()
                ));
            }
        }
        result.sort(Comparator.comparing(CalendarFixedEvent::startAt)
                .thenComparing(CalendarFixedEvent::endAt)
                .thenComparing(CalendarFixedEvent::fixedEventId));
        return result;
    }

    private void validateRequest(FixedEventRequest request) {
        if (request.effectiveFrom() != null && request.effectiveUntil() != null
                && request.effectiveFrom().isAfter(request.effectiveUntil())) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }

        if (request.recurrenceType() == RecurrenceType.ONCE) {
            if (request.dayOfWeek() != null || request.startTime() != null || request.endTime() != null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
            if (request.startAt() == null || request.endAt() == null || !request.startAt().isBefore(request.endAt())) {
                throw new AppException(ErrorCode.INVALID_TIME_RANGE);
            }
        } else if (request.recurrenceType() == RecurrenceType.WEEKLY) {
            if (request.startAt() != null || request.endAt() != null) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
            if (request.startTime() == null || request.endTime() == null || !request.startTime().isBefore(request.endTime())) {
                throw new AppException(ErrorCode.INVALID_TIME_RANGE);
            }
        }
    }

    private void applyRequest(FixedEventEntity entity, FixedEventRequest request) {
        entity.setTitle(request.title().trim());
        entity.setNote(request.note());
        entity.setRecurrenceType(request.recurrenceType());
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setEffectiveUntil(request.effectiveUntil());

        if (request.recurrenceType() == RecurrenceType.ONCE) {
            entity.setStartAt(request.startAt());
            entity.setEndAt(request.endAt());
            entity.setDayOfWeek(null);
            entity.setStartTime(null);
            entity.setEndTime(null);
        } else {
            entity.setDayOfWeek(request.dayOfWeek().shortValue());
            entity.setStartTime(request.startTime());
            entity.setEndTime(request.endTime());
            entity.setStartAt(null);
            entity.setEndAt(null);
        }
    }

    private List<String> detectSessionAffectedWarnings(Long userId, FixedEventEntity event) {
        ScheduleApi scheduleApi = scheduleApiProvider.getIfAvailable();
        if (scheduleApi == null) {
            return List.of();
        }

        ZoneId userZone = resolveUserZone(userId);
        Instant searchStart;
        Instant searchEnd;

        if (event.getRecurrenceType() == RecurrenceType.ONCE) {
            searchStart = event.getStartAt();
            searchEnd = event.getEndAt();
            if (event.getEffectiveFrom() != null) {
                Instant fromBound = event.getEffectiveFrom().atStartOfDay(userZone).toInstant();
                if (fromBound.isAfter(searchStart)) {
                    searchStart = fromBound;
                }
            }
            if (event.getEffectiveUntil() != null) {
                Instant untilBound = event.getEffectiveUntil().plusDays(1).atStartOfDay(userZone).toInstant();
                if (untilBound.isBefore(searchEnd)) {
                    searchEnd = untilBound;
                }
            }
            if (!searchStart.isBefore(searchEnd)) {
                return List.of();
            }
        } else {
            Instant now = Instant.now();
            LocalDate today = LocalDate.ofInstant(now, userZone);
            LocalDate fromDate = event.getEffectiveFrom() != null && event.getEffectiveFrom().isAfter(today)
                    ? event.getEffectiveFrom()
                    : today;
            // Bounded horizon (1 year ahead) if effectiveUntil is open-ended
            LocalDate maxHorizon = fromDate.plusYears(1);
            LocalDate untilDate = event.getEffectiveUntil() != null && event.getEffectiveUntil().isBefore(maxHorizon)
                    ? event.getEffectiveUntil()
                    : maxHorizon;
            if (fromDate.isAfter(untilDate)) {
                return List.of();
            }
            searchStart = fromDate.atStartOfDay(userZone).toInstant();
            searchEnd = untilDate.plusDays(1).atStartOfDay(userZone).toInstant();
        }

        List<ScheduledSession> committed = scheduleApi.committedSessions(userId, searchStart, searchEnd);
        if (committed == null || committed.isEmpty()) {
            return List.of();
        }

        List<TimeRange> occurrences = expandSingleEvent(event, userZone, searchStart, searchEnd);
        for (TimeRange occ : occurrences) {
            for (ScheduledSession session : committed) {
                if (occ.start().isBefore(session.endAt()) && occ.end().isAfter(session.startAt())) {
                    return List.of(WARNING_SESSION_AFFECTED);
                }
            }
        }
        return List.of();
    }

    private List<TimeRange> expandSingleEvent(FixedEventEntity event, ZoneId userZone, Instant windowStart, Instant windowEnd) {
        if (event.getRecurrenceType() == RecurrenceType.ONCE) {
            Instant occStart = event.getStartAt();
            Instant occEnd = event.getEndAt();
            if (occStart == null || occEnd == null || !occStart.isBefore(occEnd)) {
                return List.of();
            }
            if (event.getEffectiveFrom() != null) {
                LocalDate occDate = LocalDate.ofInstant(occStart, userZone);
                if (occDate.isBefore(event.getEffectiveFrom())) {
                    return List.of();
                }
            }
            if (event.getEffectiveUntil() != null) {
                LocalDate occDate = LocalDate.ofInstant(occStart, userZone);
                if (occDate.isAfter(event.getEffectiveUntil())) {
                    return List.of();
                }
            }
            if (occStart.isBefore(windowEnd) && occEnd.isAfter(windowStart)) {
                return List.of(new TimeRange(occStart, occEnd));
            }
            return List.of();
        }

        // WEEKLY
        int targetDow = event.getDayOfWeek().intValue();
        LocalDate firstDate = LocalDate.ofInstant(windowStart, userZone);
        LocalDate lastDate = LocalDate.ofInstant(windowEnd.minusNanos(1), userZone);

        if (event.getEffectiveFrom() != null && event.getEffectiveFrom().isAfter(firstDate)) {
            firstDate = event.getEffectiveFrom();
        }
        if (event.getEffectiveUntil() != null && event.getEffectiveUntil().isBefore(lastDate)) {
            lastDate = event.getEffectiveUntil();
        }
        if (firstDate.isAfter(lastDate)) {
            return List.of();
        }

        List<TimeRange> results = new ArrayList<>();
        for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() != targetDow) {
                continue;
            }
            Instant occStart = ZonedDateTime.of(date, event.getStartTime(), userZone).toInstant();
            Instant occEnd = ZonedDateTime.of(date, event.getEndTime(), userZone).toInstant();
            if (occStart.isBefore(windowEnd) && occEnd.isAfter(windowStart)) {
                results.add(new TimeRange(occStart, occEnd));
            }
        }
        return results;
    }

    ZoneId resolveUserZone(Long userId) {
        UserApi userApi = userApiProvider.getIfAvailable();
        if (userApi == null) {
            return ZoneId.of(DateUtils.DEFAULT_TIMEZONE);
        }
        return userApi.findById(userId)
                .map(UserSummary::timezone)
                .filter(DateUtils::isValidTimezone)
                .map(ZoneId::of)
                .orElse(ZoneId.of(DateUtils.DEFAULT_TIMEZONE));
    }

    private FixedEventResponse toResponse(FixedEventEntity entity, List<String> warnings) {
        return new FixedEventResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getNote(),
                entity.getRecurrenceType(),
                entity.getDayOfWeek() != null ? entity.getDayOfWeek().intValue() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getEffectiveFrom(),
                entity.getEffectiveUntil(),
                warnings,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}