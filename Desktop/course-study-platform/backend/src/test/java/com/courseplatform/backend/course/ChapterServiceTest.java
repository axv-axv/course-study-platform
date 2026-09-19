package com.courseplatform.backend.course;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChapterServiceTest {
    private final ChapterRepository repository = mock(ChapterRepository.class);
    private final CourseService courses = mock(CourseService.class);
    private final ChapterService service = new ChapterService(repository, courses);
    private final AuthenticatedUser teacher = new AuthenticatedUser(7, "teacher", UserRole.TEACHER);

    @Test
    void creatorCanCreateChapter() {
        service.create(3, teacher, new CreateChapterRequest(" 第一章 ", "简介"));

        verify(courses).requireManageable(3, teacher);
        verify(repository).create(3, "第一章", "简介");
    }

    @Test
    void reorderRequiresTheCompleteChapterSet() {
        Instant now = Instant.parse("2026-09-19T00:00:00Z");
        when(repository.findByCourse(3)).thenReturn(List.of(
                new ChapterResponse(10, 3, "一", null, 0, 0, now, now),
                new ChapterResponse(11, 3, "二", null, 1, 0, now, now)));

        assertThatThrownBy(() -> service.reorder(3, teacher, new ReorderChaptersRequest(List.of(10L, 10L))))
                .isInstanceOf(BusinessException.class).hasMessage("chapterIds 必须完整且不能重复");
    }
}
