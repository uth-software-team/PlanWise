package group16.webapp.modules.availability.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.util.SecurityUtils;
import group16.webapp.modules.availability.dto.CalendarResponse;
import group16.webapp.modules.availability.service.CalendarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * UC-10 read-only calendar of the authenticated user (E4, CAL-01): fixed-event occurrences and CONFIRMED/COMPLETED
 * sessions in {@code [start, end)}. Only the caller's own data. 400 INVALID_TIME_RANGE when the window is empty,
 * reversed or longer than 366 days (D14).
 */
@RestController
@RequestMapping("/v1/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public ApiResponse<CalendarResponse> getCalendar(@RequestParam Instant start, @RequestParam Instant end) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(calendarService.getCalendar(userId, start, end));
    }
}
