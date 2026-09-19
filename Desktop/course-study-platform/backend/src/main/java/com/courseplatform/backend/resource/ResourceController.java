package com.courseplatform.backend.resource;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import com.courseplatform.backend.common.api.PageResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ResourceController {
    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResourceResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody CreateResourceRequest request) {
        return ok(service.create(user, request));
    }

    @GetMapping("/resources/{id}")
    public ApiResponse<ResourceResponse> detail(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.detail(id, user));
    }

    @PatchMapping("/resources/{id}")
    public ApiResponse<ResourceResponse> update(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody UpdateResourceRequest request) {
        return ok(service.update(id, user, request));
    }

    @DeleteMapping("/resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(id, user);
    }

    @GetMapping("/courses/{courseId}/resources")
    public ApiResponse<PageResult<ResourceResponse>> courseResources(
            @PathVariable long courseId, @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long chapterId, @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) Long tagId, @RequestParam(required = false) String keyword
    ) {
        return ok(service.courseResources(courseId, user, page, size, chapterId, type, tagId, keyword));
    }

    @GetMapping("/chapters/{chapterId}/resources")
    public ApiResponse<PageResult<ResourceResponse>> chapterResources(
            @PathVariable long chapterId, @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ResourceType type, @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword
    ) {
        return ok(service.chapterResources(chapterId, user, page, size, type, tagId, keyword));
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagResponse>> tags(@RequestParam(required = false) String keyword) {
        return ok(service.tags(keyword));
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TagResponse> createTag(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreateTagRequest request) {
        return ok(service.createTag(user, request));
    }

    @PutMapping("/resources/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTags(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user,
                        @Valid @RequestBody SetResourceTagsRequest request) {
        service.setTags(id, user, request);
    }

    @DeleteMapping("/resources/{id}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTag(@PathVariable long id, @PathVariable long tagId,
                          @AuthenticationPrincipal AuthenticatedUser user) {
        service.removeTag(id, tagId, user);
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
