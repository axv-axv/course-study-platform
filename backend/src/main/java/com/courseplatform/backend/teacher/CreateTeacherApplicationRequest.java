package com.courseplatform.backend.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeacherApplicationRequest(
        @NotBlank @Size(min = 10, max = 1000) String reason
) {
}
