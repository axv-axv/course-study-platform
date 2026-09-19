package com.courseplatform.backend.course;

import java.time.Instant;

public record ChapterResponse(
        long id,
        long courseId,
        String title,
        String description,
        int sortOrder,
        long resourceCount,
        Instant createdAt,
        Instant updatedAt
) {
}
