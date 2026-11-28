package group16.webapp.common.util;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Time helpers. Instants are stored as TIMESTAMPTZ (UTC); calendar logic always uses the
 * user's own IANA timezone (users.timezone), never a hardcoded zone.
 * Display formatting belongs to the frontend.
 */
public final class DateUtils {

    /** Default for new users when no timezone is supplied (matches the users.timezone DB default). */
    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    /**
     * Longest query window accepted by calendar, group availability and schedule generate (TV1, 2026-11-28).
     * Exactly 366 days is allowed; anything longer is 400 INVALID_TIME_RANGE.
     */
    public static final Duration MAX_QUERY_RANGE = Duration.ofDays(366);

    private DateUtils() {}

    /**
     * True when {@code end - start} is longer than {@link #MAX_QUERY_RANGE}, compared at full Instant
     * precision (one nanosecond over is too long). Callers check {@code start < end} first.
     */
    public static boolean exceedsMaxQueryRange(Instant start, Instant end) {
        return Duration.between(start, end).compareTo(MAX_QUERY_RANGE) > 0;
    }

    /** True for a valid IANA region id such as "Asia/Ho_Chi_Minh" or "Europe/Paris". */
    public static boolean isValidTimezone(String timezone) {
        if (timezone == null || timezone.isBlank() || !timezone.contains("/")) {
            return false;
        }
        try {
            ZoneId.of(timezone);
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }

    /** Resolves the user's timezone; throws IllegalArgumentException for an invalid id. */
    public static ZoneId zoneOf(String timezone) {
        if (!isValidTimezone(timezone)) {
            throw new IllegalArgumentException("Invalid IANA timezone: " + timezone);
        }
        return ZoneId.of(timezone);
    }

    /** "Today" for a user, in that user's timezone. */
    public static LocalDate today(Instant now, ZoneId userZone) {
        return LocalDate.ofInstant(now, userZone);
    }

    /**
     * True when a deadline exists and has passed at {@code now}. A deadline is an exact instant, so
     * no timezone is needed. Overdue (BR-03) additionally requires an unfinished task status
     * (not COMPLETED or CANCELLED); that check belongs to the Task module, which owns the status enum.
     */
    public static boolean isPastDeadline(Instant deadline, Instant now) {
        return deadline != null && now.isAfter(deadline);
    }
}
