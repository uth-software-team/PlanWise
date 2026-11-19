package group16.webapp.modules.availability.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.availability.dto.FixedEventRequest;
import group16.webapp.modules.availability.dto.FixedEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UC-08 fixed events (ONCE/WEEKLY). Contract stubs (TK-01): every method answers 501 NOT_IMPLEMENTED until AV-03.
 * Planned: owner-only (another user's event is 404); start &gt;= end or effectiveFrom &gt; effectiveUntil is
 * 400 INVALID_TIME_RANGE; fields of the other recurrence type are 400 INVALID_INPUT; overlapping a
 * CONFIRMED/COMPLETED session adds the SESSION_AFFECTED warning (HC-09) without blocking the change.
 */
@RestController
@RequestMapping("/v1/fixed-events")
public class FixedEventController {

    @GetMapping
    public ApiResponse<List<FixedEventResponse>> listFixedEvents() {
        throw notImplemented();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FixedEventResponse> createFixedEvent(@Valid @RequestBody FixedEventRequest request) {
        throw notImplemented();
    }

    @PutMapping("/{id}")
    public ApiResponse<FixedEventResponse> updateFixedEvent(@PathVariable Long id,
                                                           @Valid @RequestBody FixedEventRequest request) {
        throw notImplemented();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFixedEvent(@PathVariable Long id) {
        throw notImplemented();
    }

    private static AppException notImplemented() {
        return new AppException(ErrorCode.NOT_IMPLEMENTED);
    }
}
