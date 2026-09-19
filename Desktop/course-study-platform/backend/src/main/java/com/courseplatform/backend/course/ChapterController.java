package com.courseplatform.backend.course;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ChapterController {
    private final ChapterService service;

    public ChapterController(ChapterService service) {
        this.service = service;
    }

    @PostMapping("/courses/{courseId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChapterResponse> create(@PathVariable long courseId,
                                                @AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody CreateChapterRequest request) {
        return ok(service.create(courseId, user, request));
    }

    @GetMapping("/courses/{courseId}/chapters")
    public ApiResponse<List<ChapterResponse>> list(@PathVariable long courseId,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.list(courseId, user));
    }

    @GetMapping("/chapters/{id}")
    public ApiResponse<ChapterResponse> detail(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.detail(id, user));
    }

    @PatchMapping("/chapters/{id}")
    public ApiResponse<ChapterResponse> update(@PathVariable long id,
                                                @AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody UpdateChapterRequest request) {
        return ok(service.update(id, user, request));
    }

    @DeleteMapping("/chapters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(id, user);
    }

    @PutMapping("/courses/{courseId}/chapters/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable long courseId, @AuthenticationPrincipal AuthenticatedUser user,
                        @Valid @RequestBody ReorderChaptersRequest request) {
        service.reorder(courseId, user, request);
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
