package com.courseplatform.backend.teacher;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import com.courseplatform.backend.common.api.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/teacher-applications")
public class AdminTeacherApplicationController {
    private final TeacherApplicationService service;

    public AdminTeacherApplicationController(TeacherApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<TeacherApplication>> list(
            @RequestParam(required = false) TeacherApplicationStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.list(status, page, size), MDC.get("traceId"));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<TeacherApplication> approve(
            @PathVariable long id,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @Valid @RequestBody(required = false) ReviewTeacherApplicationRequest request
    ) {
        ReviewTeacherApplicationRequest body = request == null ? new ReviewTeacherApplicationRequest(null) : request;
        return ApiResponse.success(service.approve(id, admin, body), MDC.get("traceId"));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<TeacherApplication> reject(
            @PathVariable long id,
            @AuthenticationPrincipal AuthenticatedUser admin,
            @Valid @RequestBody(required = false) ReviewTeacherApplicationRequest request
    ) {
        ReviewTeacherApplicationRequest body = request == null ? new ReviewTeacherApplicationRequest(null) : request;
        return ApiResponse.success(service.reject(id, admin, body), MDC.get("traceId"));
    }
}
