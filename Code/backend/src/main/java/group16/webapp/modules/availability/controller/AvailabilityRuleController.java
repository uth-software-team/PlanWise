package group16.webapp.modules.availability.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.util.SecurityUtils;
import group16.webapp.modules.availability.dto.AvailabilityRuleRequest;
import group16.webapp.modules.availability.dto.AvailabilityRuleResponse;
import group16.webapp.modules.availability.service.AvailabilityService;
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
 * UC-07 weekly availability rules (AV-02).
 */
@RestController
@RequestMapping("/v1/availability-rules")
public class AvailabilityRuleController {

    private final AvailabilityService availabilityService;

    public AvailabilityRuleController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ApiResponse<List<AvailabilityRuleResponse>> listRules() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(availabilityService.listRules(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AvailabilityRuleResponse> createRule(@Valid @RequestBody AvailabilityRuleRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(availabilityService.createRule(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AvailabilityRuleResponse> updateRule(@PathVariable Long id,
                                                           @Valid @RequestBody AvailabilityRuleRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(availabilityService.updateRule(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        availabilityService.deleteRule(userId, id);
    }
}