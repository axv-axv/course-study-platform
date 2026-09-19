package com.courseplatform.backend.learning;

public record CourseProgressResponse(
        long resourceCount,
        long completedCount,
        long inProgressCount,
        long notStartedCount,
        int progress
) {
}
