package com.courseplatform.backend.course;

import jakarta.validation.constraints.Size;

public record UpdateChapterRequest(
        @Size(min = 1, max = 100) String title,
        @Size(max = 500) String description
) {
}
