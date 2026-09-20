package com.courseplatform.backend.course;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.learning.CourseProgressResponse;
import com.courseplatform.backend.learning.LearningRepository;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseOverviewServiceTest {
    private final CourseService courses = mock(CourseService.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final LearningRepository learning = mock(LearningRepository.class);
    private final CourseOverviewService service = new CourseOverviewService(courses, chapters, resources, learning);

    @Test
    void aggregatesCourseHomeDataAndAiAvailability() {
        AuthenticatedUser user = new AuthenticatedUser(8, "student", UserRole.STUDENT);
        CourseResponse course = mock(CourseResponse.class);
        CourseProgressResponse progress = new CourseProgressResponse(5, 2, 1, 2, 40);
        ResourceResponse resource = mock(ResourceResponse.class);
        when(courses.requireViewable(11, user)).thenReturn(course);
        when(chapters.findByCourse(11)).thenReturn(List.of());
        when(learning.courseProgress(8, 11)).thenReturn(progress);
        when(resources.findPage(11, null, null, null, null, 1, 6))
                .thenReturn(new PageResult<>(List.of(resource), 1, 6, 1));
        when(resources.hasIndexedResources(11)).thenReturn(true);

        CourseOverviewResponse response = service.overview(11, user);

        verify(courses).requireViewable(11, user);
        assertThat(response.course()).isSameAs(course);
        assertThat(response.progress()).isEqualTo(progress);
        assertThat(response.recentResources()).containsExactly(resource);
        assertThat(response.aiEnabled()).isTrue();
    }
}
