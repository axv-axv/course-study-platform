package com.courseplatform.backend.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChapterRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 500) String description
) {
}
