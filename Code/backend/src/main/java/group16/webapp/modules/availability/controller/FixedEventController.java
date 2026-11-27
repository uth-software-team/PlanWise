package group16.webapp.modules.availability.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.util.SecurityUtils;
import group16.webapp.modules.availability.dto.FixedEventRequest;
import group16.webapp.modules.availability.dto.FixedEventResponse;
import group16.webapp.modules.availability.service.FixedEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/fixed-events")
public class FixedEventController {

    private final FixedEventService fixedEventService;

    public FixedEventController(FixedEventService fixedEventService) {
        this.fixedEventService = fixedEventService;
    }

    @GetMapping
    public ApiResponse<List<FixedEventResponse>> listFixedEvents() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fixedEventService.listFixedEvents(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FixedEventResponse> createFixedEvent(@Valid @RequestBody FixedEventRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fixedEventService.createFixedEvent(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<FixedEventResponse> updateFixedEvent(@PathVariable Long id,
                                                           @Valid @RequestBody FixedEventRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fixedEventService.updateFixedEvent(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFixedEvent(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        fixedEventService.deleteFixedEvent(userId, id);
    }
}