package com.courseplatform.backend.learning;

import java.time.Instant;

public record ProgressResponse(
        StudyStatus status,
        int progress,
        LearningPosition position,
        Instant lastStudyAt
) {
    public static ProgressResponse notStarted() {
        return new ProgressResponse(StudyStatus.NOT_STARTED, 0, null, null);
    }
}
