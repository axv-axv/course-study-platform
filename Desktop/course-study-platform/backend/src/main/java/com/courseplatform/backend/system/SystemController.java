package com.courseplatform.backend.system;

import com.courseplatform.backend.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping(
            @RequestParam(defaultValue = "pong") @NotBlank @Size(max = 32) String message
    ) {
        return ApiResponse.success(Map.of(
                "service", "course-study-platform-backend",
                "message", message
        ), MDC.get("traceId"));
    }
}
