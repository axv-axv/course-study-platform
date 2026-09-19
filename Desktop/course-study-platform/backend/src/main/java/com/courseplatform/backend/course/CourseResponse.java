package com.courseplatform.backend.course;

import java.time.Instant;

public record CourseResponse(
        long id,
        String title,
        String description,
        String coverUrl,
        long creatorId,
        CourseVisibility visibility,
        CourseStatus status,
        long memberCount,
        long resourceCount,
        long chapterCount,
        Creator creator,
        boolean joined,
        Instant createdAt,
        Instant updatedAt
) {
    public record Creator(long id, String nickname) {
    }
}
