package com.courseplatform.backend.rag;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources/{resourceId}")
public class RagIndexController {
    private final RagIndexService service;

    public RagIndexController(RagIndexService service) {
        this.service = service;
    }

    @GetMapping("/ai-status")
    public ApiResponse<AiIndexStatusResponse> status(@PathVariable long resourceId,
                                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.status(resourceId, user));
    }

    @PostMapping("/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AiIndexStatusResponse> reindex(@PathVariable long resourceId,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.reindex(resourceId, user));
    }

    @DeleteMapping("/ai-index")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long resourceId, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(resourceId, user);
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
