package group16.webapp.modules.availability.service;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.AvailabilityApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.dto.AvailabilityRuleRequest;
import group16.webapp.modules.availability.dto.AvailabilityRuleResponse;
import group16.webapp.modules.availability.entity.AvailabilityRuleEntity;
import group16.webapp.modules.availability.repository.AvailabilityRuleRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AvailabilityService implements AvailabilityApi {

    private final AvailabilityRuleRepository ruleRepository;
    private final FixedEventService fixedEventService;
    private final ObjectProvider<UserApi> userApiProvider;

    public AvailabilityService(
            AvailabilityRuleRepository ruleRepository,
            FixedEventService fixedEventService,
            ObjectProvider<UserApi> userApiProvider
    ) {
        this.ruleRepository = ruleRepository;
        this.fixedEventService = fixedEventService;
        this.userApiProvider = userApiProvider;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRuleResponse> listRules(Long userId) {
        return ruleRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AvailabilityRuleResponse createRule(Long userId, AvailabilityRuleRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        AvailabilityRuleEntity entity = new AvailabilityRuleEntity();
        entity.setUserId(userId);
        entity.setDayOfWeek(request.dayOfWeek().shortValue());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setActive(request.active() != null ? request.active() : true);

        AvailabilityRuleEntity saved = ruleRepository.saveAndFlush(entity);
        return toResponse(saved);
    }

    @Transactional
    public AvailabilityRuleResponse updateRule(Long userId, Long ruleId, AvailabilityRuleRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        AvailabilityRuleEntity entity = ruleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        entity.setDayOfWeek(request.dayOfWeek().shortValue());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setActive(request.active() != null ? request.active() : true);

        AvailabilityRuleEntity saved = ruleRepository.saveAndFlush(entity);
        return toResponse(saved);
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        AvailabilityRuleEntity entity = ruleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        ruleRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveRule(Long userId) {
        return ruleRepository.existsByUserIdAndActiveTrue(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> availableIntervals(Long userId, Instant start, Instant end) {
        if (start == null || end == null || !start.isBefore(end)) {
            return List.of();
        }

        List<AvailabilityRuleEntity> activeRules = ruleRepository.findByUserIdAndActiveTrue(userId);
        if (activeRules.isEmpty()) {
            return List.of();
        }

        ZoneId userZone = resolveUserZone(userId);
        Map<Integer, List<AvailabilityRuleEntity>> rulesByDay = activeRules.stream()
                .collect(Collectors.groupingBy(r -> r.getDayOfWeek().intValue()));

        LocalDate firstDate = LocalDate.ofInstant(start, userZone);
        LocalDate lastDate = LocalDate.ofInstant(end.minusNanos(1), userZone);

        List<TimeRange> rawRanges = new ArrayList<>();
        for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
            List<AvailabilityRuleEntity> dayRules = rulesByDay.get(date.getDayOfWeek().getValue());
            if (dayRules == null) {
                continue;
            }
            for (AvailabilityRuleEntity rule : dayRules) {
                Instant ruleStart = ZonedDateTime.of(date, rule.getStartTime(), userZone).toInstant();
                Instant ruleEnd = ZonedDateTime.of(date, rule.getEndTime(), userZone).toInstant();

                Instant clippedStart = ruleStart.isAfter(start) ? ruleStart : start;
                Instant clippedEnd = ruleEnd.isBefore(end) ? ruleEnd : end;

                if (clippedStart.isBefore(clippedEnd)) {
                    rawRanges.add(new TimeRange(clippedStart, clippedEnd));
                }
            }
        }

        return mergeRanges(rawRanges);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeRange> fixedEventOccurrences(Long userId, Instant start, Instant end) {
        return fixedEventService.expandMergedOccurrences(userId, start, end);
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }
    }

    private ZoneId resolveUserZone(Long userId) {
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

    static List<TimeRange> mergeRanges(List<TimeRange> ranges) {
        if (ranges.size() <= 1) {
            return List.copyOf(ranges);
        }
        List<TimeRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(TimeRange::start).thenComparing(TimeRange::end));

        List<TimeRange> merged = new ArrayList<>();
        TimeRange current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            TimeRange next = sorted.get(i);
            if (!next.start().isAfter(current.end())) {
                Instant newEnd = next.end().isAfter(current.end()) ? next.end() : current.end();
                current = new TimeRange(current.start(), newEnd);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private AvailabilityRuleResponse toResponse(AvailabilityRuleEntity entity) {
        return new AvailabilityRuleResponse(
                entity.getId(),
                entity.getDayOfWeek().intValue(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}