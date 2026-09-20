package com.courseplatform.backend.ai;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import com.courseplatform.backend.common.api.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiService service;

    public AiController(AiService service) {
        this.service = service;
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateAiConversationResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @Valid @RequestBody CreateAiConversationRequest request) {
        return ok(service.create(user, request));
    }

    @GetMapping("/conversations")
    public ApiResponse<PageResult<AiConversationResponse>> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @RequestParam(required = false) Long courseId,
                                                                 @RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        return ok(service.list(user, courseId, page, size));
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<AiConversationResponse> detail(@PathVariable long id,
                                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.detail(id, user));
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(id, user);
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody AiChatRequest request) {
        return ok(service.chat(user, request));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
