package group16.webapp.modules.ai.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.ai.dto.TaskDraftResponse;
import group16.webapp.modules.ai.dto.TaskParseRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-02 AI task parsing. Contract stub (GR-01): answers 501 NOT_IMPLEMENTED until AI-02. Planned: returns a
 * draft only (no task is saved); provider, timeout and invalid provider JSON failures are 503 AI_UNAVAILABLE.
 */
@RestController
@RequestMapping("/v1/ai")
public class AiController {

    @PostMapping("/tasks/parse")
    public ApiResponse<TaskDraftResponse> parseTask(@Valid @RequestBody TaskParseRequest request) {
        throw new AppException(ErrorCode.NOT_IMPLEMENTED);
    }
}
