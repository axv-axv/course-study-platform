package com.courseplatform.backend.course;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.learning.LearningRepository;
import com.courseplatform.backend.resource.ResourceRepository;
import org.springframework.stereotype.Service;

@Service
public class CourseOverviewService {
    private final CourseService courses;
    private final ChapterRepository chapters;
    private final ResourceRepository resources;
    private final LearningRepository learning;

    public CourseOverviewService(CourseService courses, ChapterRepository chapters,
                                 ResourceRepository resources, LearningRepository learning) {
        this.courses = courses;
        this.chapters = chapters;
        this.resources = resources;
        this.learning = learning;
    }

    public CourseOverviewResponse overview(long courseId, AuthenticatedUser user) {
        CourseResponse course = courses.requireViewable(courseId, user);
        return new CourseOverviewResponse(
                course,
                chapters.findByCourse(courseId),
                learning.courseProgress(user.id(), courseId),
                resources.findPage(courseId, null, null, null, null, 1, 6).items(),
                resources.hasIndexedResources(courseId)
        );
    }
}
