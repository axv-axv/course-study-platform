package com.courseplatform.backend.course;

import com.courseplatform.backend.learning.CourseProgressResponse;
import com.courseplatform.backend.resource.ResourceResponse;

import java.util.List;

public record CourseOverviewResponse(
        CourseResponse course,
        List<ChapterResponse> chapters,
        CourseProgressResponse progress,
        List<ResourceResponse> recentResources,
        boolean aiEnabled
) {
}
