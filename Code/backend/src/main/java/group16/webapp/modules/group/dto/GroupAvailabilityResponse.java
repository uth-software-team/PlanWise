package group16.webapp.modules.group.dto;

import java.time.Instant;
import java.util.List;

/**
 * CG-11 group availability: for each ACTIVE member only the user id and BUSY intervals (half-open
 * {@code [startAt, endAt)}). Never titles, notes, task ids or session details. Implemented in GR-04.
 */
public record GroupAvailabilityResponse(
        List<MemberBusy> members
) {

    public record MemberBusy(Long userId, List<BusyInterval> busy) {
    }

    public record BusyInterval(Instant startAt, Instant endAt) {
    }
}
