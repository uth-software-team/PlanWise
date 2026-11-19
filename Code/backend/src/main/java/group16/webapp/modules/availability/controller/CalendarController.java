package group16.webapp.modules.availability.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.availability.dto.CalendarResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * UC-10 read-only calendar of the authenticated user (E4). Contract stub (TK-01): answers 501 NOT_IMPLEMENTED
 * until CAL-01. Planned: start &gt;= end is 400 INVALID_TIME_RANGE; only the caller's own data is returned.
 */
@RestController
@RequestMapping("/v1/calendar")
public class CalendarController {

    @GetMapping
    public ApiResponse<CalendarResponse> getCalendar(@RequestParam Instant start, @RequestParam Instant end) {
        throw new AppException(ErrorCode.NOT_IMPLEMENTED);
    }
}
