package com.courseplatform.backend.search;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseRepository;
import com.courseplatform.backend.course.CourseStatus;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {
    private final CourseRepository courses = mock(CourseRepository.class);
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final SearchService service = new SearchService(courses, resources);
    private final AuthenticatedUser student = new AuthenticatedUser(8, "student", UserRole.STUDENT);

    @Test
    void unifiedSearchQueriesAllThreeKinds() {
        when(courses.findVisiblePage(8, false, 1, 10, "Java", null, null, CourseStatus.ACTIVE))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));
        when(resources.findSearchPage(8, false, "Java", null, null, null, null, null, "newest", 1, 10))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));
        when(resources.findTagsPage("Java", 1, 10))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0));

        service.unified(student, " Java ", null, 1, 10);

        verify(courses).findVisiblePage(8, false, 1, 10, "Java", null, null, CourseStatus.ACTIVE);
        verify(resources).findSearchPage(8, false, "Java", null, null, null, null, null, "newest", 1, 10);
        verify(resources).findTagsPage("Java", 1, 10);
    }

    @Test
    void rejectsUnsupportedResourceSort() {
        assertThatThrownBy(() -> service.resources(student, null, null, null, null, null, null,
                "unsafe sql", 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessage("sort 仅支持 newest、popular、downloads 或 title");
    }

    @Test
    void unifiedSearchRequiresKeyword() {
        assertThatThrownBy(() -> service.unified(student, " ", null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessage("keyword 不能为空");
    }
}
