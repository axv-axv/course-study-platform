package com.courseplatform.backend.teacher;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TeacherApplicationController {
    private final TeacherApplicationService service;

    public TeacherApplicationController(TeacherApplicationService service) {
        this.service = service;
    }

    @PostMapping("/teacher-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherApplication> apply(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateTeacherApplicationRequest request
    ) {
        return ApiResponse.success(service.apply(principal, request), MDC.get("traceId"));
    }

    @GetMapping("/users/me/teacher-application")
    public ApiResponse<TeacherApplication> latest(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(service.latest(principal), MDC.get("traceId"));
    }
}
