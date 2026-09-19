package com.courseplatform.backend.learning;

import com.courseplatform.backend.resource.ResourceType;

import java.time.Instant;

public record NoteResponse(
        long id,
        long userId,
        long resourceId,
        String content,
        LearningPosition position,
        Instant createdAt,
        Instant updatedAt,
        ResourceRef resource
) {
    public record ResourceRef(long id, String title, ResourceType resourceType, String courseTitle) {}
}
