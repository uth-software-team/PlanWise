package group16.webapp.modules.group.service;

import group16.webapp.modules.group.dto.GroupAvailabilityResponse;

import java.time.Instant;

/** CG-11 group availability (GR-04): BUSY intervals only, never titles, notes or ids of the underlying items. */
public interface GroupAvailabilityService {

    /**
     * BUSY intervals of every ACTIVE member of the group within {@code [start, end)}, in membership order. Each
     * member's intervals are clipped to the window, sorted and merged (overlapping or adjacent); members without busy
     * time have an empty list.
     * <p>
     * Checks in this order, before any calendar lookup: 401 UNAUTHENTICATED when the caller no longer exists or is
     * inactive, 400 INVALID_TIME_RANGE when {@code start >= end}, 404 RESOURCE_NOT_FOUND for a missing group,
     * 403 FORBIDDEN when the caller is not an ACTIVE member. Then 501 NOT_IMPLEMENTED while no calendar provider
     * (CAL-01) is available.
     */
    GroupAvailabilityResponse getAvailability(Long callerId, Long groupId, Instant start, Instant end);
}
