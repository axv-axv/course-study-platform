package com.courseplatform.backend.admin;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.course.CourseResponse;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.user.UserResponse;
import com.courseplatform.backend.user.UserRole;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<UserResponse>> users(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role) {
        return ok(service.users(page, size, keyword, role));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserResponse> user(@PathVariable long userId) {
        return ok(service.user(userId));
    }

    @PatchMapping("/users/{userId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable long userId, @AuthenticationPrincipal AuthenticatedUser operator,
                             @Valid @RequestBody UpdateUserStatusRequest request) {
        service.updateStatus(userId, operator, request);
    }

    @PatchMapping("/users/{userId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRole(@PathVariable long userId, @AuthenticationPrincipal AuthenticatedUser operator,
                           @Valid @RequestBody UpdateUserRoleRequest request) {
        service.updateRole(userId, operator, request);
    }

    @GetMapping("/courses")
    public ApiResponse<PageResult<CourseResponse>> courses(@AuthenticationPrincipal AuthenticatedUser operator,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ok(service.courses(operator, page, size, keyword));
    }

    @DeleteMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable long courseId) {
        service.deleteCourse(courseId);
    }

    @GetMapping("/resources")
    public ApiResponse<PageResult<ResourceResponse>> resources(@AuthenticationPrincipal AuthenticatedUser operator,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Long courseId) {
        return ok(service.resources(operator, page, size, keyword, courseId));
    }

    @DeleteMapping("/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable long resourceId) {
        service.deleteResource(resourceId);
    }

    @GetMapping("/statistics")
    public ApiResponse<AdminStatisticsResponse> statistics() {
        return ok(service.statistics());
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
