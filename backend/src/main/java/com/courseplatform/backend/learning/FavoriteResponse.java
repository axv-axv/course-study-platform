package com.courseplatform.backend.learning;

import com.courseplatform.backend.resource.ResourceType;

public record FavoriteResponse(
        long id,
        String title,
        String description,
        ResourceType resourceType,
        CourseRef course,
        String courseTitle,
        int progress
) {
    public record CourseRef(long id, String title) {}
}
