package group16.webapp.modules.availability.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.availability.dto.CalendarResponse;
import group16.webapp.modules.availability.service.CalendarService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/v1/calendar")
public class CalendarController {

    private final ObjectProvider<CalendarService> calendarServiceProvider;

    public CalendarController(ObjectProvider<CalendarService> calendarServiceProvider) {
        this.calendarServiceProvider = calendarServiceProvider;
    }

    @GetMapping
    public ApiResponse<CalendarResponse> getCalendar(@RequestParam Instant start, @RequestParam Instant end) {
        CalendarService service = calendarServiceProvider.getIfAvailable();
        if (service == null) {
            throw new AppException(ErrorCode.NOT_IMPLEMENTED);
        }
        Long userId = group16.webapp.common.util.SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(service.getCalendar(userId, start, end));
    }
}