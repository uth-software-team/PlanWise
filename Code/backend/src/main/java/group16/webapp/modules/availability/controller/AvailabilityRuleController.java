package group16.webapp.modules.availability.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.availability.dto.AvailabilityRuleRequest;
import group16.webapp.modules.availability.dto.AvailabilityRuleResponse;
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
 * UC-07 weekly availability rules. Contract stubs (TK-01): every method answers 501 NOT_IMPLEMENTED until AV-02.
 * Planned: owner-only (another user's rule is 404); startTime &gt;= endTime is 400 INVALID_TIME_RANGE.
 */
@RestController
@RequestMapping("/v1/availability-rules")
public class AvailabilityRuleController {

    @GetMapping
    public ApiResponse<List<AvailabilityRuleResponse>> listRules() {
        throw notImplemented();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AvailabilityRuleResponse> createRule(@Valid @RequestBody AvailabilityRuleRequest request) {
        throw notImplemented();
    }

    @PutMapping("/{id}")
    public ApiResponse<AvailabilityRuleResponse> updateRule(@PathVariable Long id,
                                                           @Valid @RequestBody AvailabilityRuleRequest request) {
        throw notImplemented();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        throw notImplemented();
    }

    private static AppException notImplemented() {
        return new AppException(ErrorCode.NOT_IMPLEMENTED);
    }
}
