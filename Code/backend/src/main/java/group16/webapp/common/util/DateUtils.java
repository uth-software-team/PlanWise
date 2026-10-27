package group16.webapp.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtils() {}

    public static LocalDate today() {
        return LocalDate.now(VN_ZONE);
    }

    public static String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMAT);
    }

    public static String format(Instant instant) {
        return instant == null ? null : instant.atZone(VN_ZONE).format(DATE_TIME_FORMAT);
    }

    public static boolean isOverdue(LocalDate dueDate) {
        return dueDate != null && dueDate.isBefore(today());
    }
}
