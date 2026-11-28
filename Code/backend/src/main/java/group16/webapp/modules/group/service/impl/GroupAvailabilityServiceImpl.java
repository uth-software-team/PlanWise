package group16.webapp.modules.group.service.impl;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.CalendarApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.group.api.GroupAccessApi;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse.BusyInterval;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse.MemberBusy;
import group16.webapp.modules.group.service.GroupAvailabilityService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GroupAvailabilityServiceImpl implements GroupAvailabilityService {

    private final UserApi userApi;
    private final GroupAccessApi groupAccessApi;
    /** Optional until CAL-01 provides an implementation. */
    private final ObjectProvider<CalendarApi> calendarApiProvider;

    public GroupAvailabilityServiceImpl(UserApi userApi, GroupAccessApi groupAccessApi,
                                        ObjectProvider<CalendarApi> calendarApiProvider) {
        this.userApi = userApi;
        this.groupAccessApi = groupAccessApi;
        this.calendarApiProvider = calendarApiProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public GroupAvailabilityResponse getAvailability(Long callerId, Long groupId, Instant start, Instant end) {
        userApi.findById(callerId)
                .filter(UserSummary::active)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (!start.isBefore(end) || DateUtils.exceedsMaxQueryRange(start, end)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }
        groupAccessApi.requireMember(groupId, callerId);

        CalendarApi calendarApi = calendarApiProvider.getIfAvailable();
        if (calendarApi == null) {
            throw new AppException(ErrorCode.NOT_IMPLEMENTED);
        }

        List<MemberBusy> members = groupAccessApi.memberIds(groupId).stream()
                .map(userId -> new MemberBusy(userId,
                        normalize(calendarApi.busyIntervals(userId, start, end), start, end)))
                .toList();
        return new GroupAvailabilityResponse(members);
    }

    /** Clips to {@code [start, end)}, drops empty ranges, sorts by start and merges overlapping or adjacent ones. */
    private static List<BusyInterval> normalize(List<TimeRange> ranges, Instant start, Instant end) {
        List<TimeRange> clipped = ranges.stream()
                .map(r -> new TimeRange(max(r.start(), start), min(r.end(), end)))
                .filter(r -> r.start().isBefore(r.end()))
                .sorted(Comparator.comparing(TimeRange::start))
                .toList();

        List<BusyInterval> merged = new ArrayList<>();
        Instant curStart = null;
        Instant curEnd = null;
        for (TimeRange r : clipped) {
            if (curEnd != null && !r.start().isAfter(curEnd)) {
                curEnd = max(curEnd, r.end());
            } else {
                if (curEnd != null) {
                    merged.add(new BusyInterval(curStart, curEnd));
                }
                curStart = r.start();
                curEnd = r.end();
            }
        }
        if (curEnd != null) {
            merged.add(new BusyInterval(curStart, curEnd));
        }
        return List.copyOf(merged);
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
