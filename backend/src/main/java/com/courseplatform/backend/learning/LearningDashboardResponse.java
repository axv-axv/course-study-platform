package com.courseplatform.backend.learning;

import java.util.List;

public record LearningDashboardResponse(
        long courseCount,
        long completedResourceCount,
        long favoriteCount,
        long noteCount,
        List<RecentLearningResponse> recentLearning,
        List<CourseSummary> courses
) {
    public record CourseSummary(long courseId, String title, int progress, String coverUrl) {}
}
