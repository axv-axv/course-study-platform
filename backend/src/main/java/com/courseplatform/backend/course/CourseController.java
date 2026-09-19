package com.courseplatform.backend.course;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CourseController {
    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreateCourseRequest request) {
        return ok(service.create(user, request));
    }

    @GetMapping("/courses")
    public ApiResponse<PageResult<CourseResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) CourseVisibility visibility,
            @RequestParam(required = false) CourseStatus status
    ) {
        return ok(service.list(user, page, size, keyword, creatorId, visibility, status));
    }

    @GetMapping("/courses/{id}")
    public ApiResponse<CourseResponse> detail(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.detail(id, user));
    }

    @PatchMapping("/courses/{id}")
    public ApiResponse<CourseResponse> update(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody UpdateCourseRequest request) {
        return ok(service.update(id, user, request));
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(id, user);
    }

    @PostMapping("/courses/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseMembershipResponse> join(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.join(id, user));
    }

    @DeleteMapping("/courses/{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.leave(id, user);
    }

    @GetMapping("/courses/{id}/members")
    public ApiResponse<PageResult<CourseMemberResponse>> members(
            @PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ok(service.members(id, user, page, size, keyword));
    }

    @DeleteMapping("/courses/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable long id, @PathVariable long memberId,
                             @AuthenticationPrincipal AuthenticatedUser user) {
        service.removeMember(id, memberId, user);
    }

    @GetMapping("/users/me/courses")
    public ApiResponse<PageResult<CourseResponse>> myJoined(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size
    ) {
        return ok(service.myJoined(user, page, size));
    }

    @GetMapping("/users/me/created-courses")
    public ApiResponse<PageResult<CourseResponse>> myCreated(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size
    ) {
        return ok(service.myCreated(user, page, size));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
