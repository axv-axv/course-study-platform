package com.courseplatform.backend.course;

import jakarta.validation.constraints.Size;

public record UpdateCourseRequest(
        @Size(min = 1, max = 100) String title,
        @Size(max = 500) String description,
        @Size(max = 500) String coverUrl,
        CourseVisibility visibility,
        CourseStatus status
) {
}
