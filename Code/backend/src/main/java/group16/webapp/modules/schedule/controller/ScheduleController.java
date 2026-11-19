package group16.webapp.modules.schedule.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.schedule.dto.ConfirmRequest;
import group16.webapp.modules.schedule.dto.GenerateScheduleRequest;
import group16.webapp.modules.schedule.dto.ProposalBatchResponse;
import group16.webapp.modules.schedule.dto.SessionEditRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * UC-01 / UC-04 smart scheduling (CG-07). Contract stubs (SC-01): valid authenticated requests answer 501
 * NOT_IMPLEMENTED until SC-05a (generate), SC-06 (get, discard, edit and delete sessions) and SC-07 (confirm)
 * implement them. Request validation already runs: malformed or missing input is 400 INVALID_INPUT and an empty or
 * reversed time range is 400 INVALID_TIME_RANGE.
 *
 * <p>Requirements for the implementations:
 * <ul>
 *   <li>The user is always the authenticated caller; requests carry no user id.</li>
 *   <li>Owner scope: a proposal or session of another user is 404 RESOURCE_NOT_FOUND, never 403.</li>
 *   <li>Every mutation sends the <b>batch</b> version; a mismatch is 409 STALE_VERSION, and each successful edit
 *   increments the batch version (BR-10, BR-11).</li>
 *   <li>Only PROPOSED batches and sessions can be changed; CONFIRMED/COMPLETED sessions are never touched.</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/schedules")
public class ScheduleController {

    /**
     * Creates the caller's new PROPOSED batch (replacing any previous PROPOSED batch) and returns it with the
     * unscheduled tasks. 200 even when {@code unscheduledTasks} is not empty; 400 NO_AVAILABILITY_RULE without rules.
     */
    @PostMapping("/generate")
    public ApiResponse<ProposalBatchResponse> generate(@Valid @RequestBody GenerateScheduleRequest request) {
        requireOrderedRange(request.windowStart(), request.windowEnd());
        throw notImplemented();
    }

    /** One of the caller's batches. */
    @GetMapping("/proposals/{proposalId}")
    public ApiResponse<ProposalBatchResponse> getProposal(@PathVariable Long proposalId) {
        throw notImplemented();
    }

    /** Discards the caller's PROPOSED batch ({@code version} = batch version); confirmed sessions are kept. */
    @DeleteMapping("/proposals/{proposalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardProposal(
            @PathVariable Long proposalId,
            @RequestParam @NotNull @PositiveOrZero Integer version) {
        throw notImplemented();
    }

    /** Moves one PROPOSED session of the caller; HC-01..09 are checked and the updated batch is returned. */
    @PutMapping("/sessions/{id}")
    public ApiResponse<ProposalBatchResponse> editSession(
            @PathVariable Long id, @Valid @RequestBody SessionEditRequest request) {
        requireOrderedRange(request.startAt(), request.endAt());
        throw notImplemented();
    }

    /** Removes one PROPOSED session of the caller ({@code version} = batch version) and returns the updated batch. */
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<ProposalBatchResponse> deleteSession(
            @PathVariable Long id,
            @RequestParam @NotNull @PositiveOrZero Integer version) {
        throw notImplemented();
    }

    /**
     * Confirms the caller's whole PROPOSED batch in one transaction: any violated constraint is 409
     * CONSTRAINT_VIOLATION with {@code conflicts[]} and the whole batch is rolled back.
     */
    @PostMapping("/{proposalId}/confirm")
    public ApiResponse<ProposalBatchResponse> confirm(
            @PathVariable Long proposalId, @Valid @RequestBody ConfirmRequest request) {
        throw notImplemented();
    }

    /** Present and well-formed, but empty or reversed: half-open ranges need {@code start < end}. */
    private static void requireOrderedRange(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE);
        }
    }

    private static AppException notImplemented() {
        return new AppException(ErrorCode.NOT_IMPLEMENTED);
    }
}
