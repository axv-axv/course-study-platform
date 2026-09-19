package com.courseplatform.backend.resource;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.ChapterRepository;
import com.courseplatform.backend.course.ChapterResponse;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.file.FileService;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceServiceTest {
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final CourseService courses = mock(CourseService.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final FileService files = mock(FileService.class);
    private final ResourceService service = new ResourceService(resources, courses, chapters, files);
    private final AuthenticatedUser teacher = new AuthenticatedUser(7, "teacher", UserRole.TEACHER);

    @Test
    void rejectsNonHttpExternalUrl() {
        CreateResourceRequest request = new CreateResourceRequest(1L, null, "链接", null,
                ResourceType.LINK, null, "file:///etc/passwd");

        assertThatThrownBy(() -> service.create(teacher, request))
                .isInstanceOf(BusinessException.class).hasMessage("外部链接必须是有效的 http/https 地址");
    }

    @Test
    void rejectsChapterFromAnotherCourse() {
        Instant now = Instant.parse("2026-09-19T00:00:00Z");
        when(chapters.findById(3)).thenReturn(Optional.of(
                new ChapterResponse(3, 99, "章节", null, 0, 0, now, now)));
        CreateResourceRequest request = new CreateResourceRequest(1L, 3L, "资料", null,
                ResourceType.LINK, null, "https://example.com");

        assertThatThrownBy(() -> service.create(teacher, request))
                .isInstanceOf(BusinessException.class).hasMessage("章节不属于指定课程");
    }

    @Test
    void studentCannotCreateTag() {
        AuthenticatedUser student = new AuthenticatedUser(8, "student", UserRole.STUDENT);
        assertThatThrownBy(() -> service.createTag(student, new CreateTagRequest("Java")))
                .isInstanceOf(BusinessException.class).hasMessage("仅教师或管理员可以创建标签");
    }
}
