package com.courseplatform.backend.search;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.course.CourseResponse;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.resource.ResourceType;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UnifiedSearchResponse> unified(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String keyword, @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ok(service.unified(user, keyword, type, page, size));
    }

    @GetMapping("/courses")
    public ApiResponse<PageResult<CourseResponse>> courses(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String keyword, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(service.courses(user, keyword, page, size));
    }

    @GetMapping("/resources")
    public ApiResponse<PageResult<ResourceResponse>> resources(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long chapterId, @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) Long tagId, @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String sort, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(service.resources(user, keyword, courseId, chapterId, resourceType, tagId, creatorId, sort, page, size));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
