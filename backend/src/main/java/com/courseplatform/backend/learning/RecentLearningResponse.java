package com.courseplatform.backend.learning;

import com.courseplatform.backend.resource.ResourceType;

import java.time.Instant;

public record RecentLearningResponse(
        long resourceId,
        String title,
        int progress,
        long courseId,
        String courseTitle,
        ResourceType resourceType,
        Instant updatedAt
) {
}
