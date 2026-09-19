package com.courseplatform.backend.course;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseServiceTest {
    private final CourseRepository repository = mock(CourseRepository.class);
    private final CourseService service = new CourseService(repository);

    @Test
    void teacherCanCreateCourse() {
        AuthenticatedUser teacher = new AuthenticatedUser(7, "teacher", UserRole.TEACHER);
        CourseResponse created = course(1, 7, CourseVisibility.PUBLIC, CourseStatus.ACTIVE, false);
        when(repository.create(7, "Java 基础", null, CourseVisibility.PUBLIC)).thenReturn(created);

        service.create(teacher, new CreateCourseRequest(" Java 基础 ", null, null));

        verify(repository).create(7, "Java 基础", null, CourseVisibility.PUBLIC);
    }

    @Test
    void studentCannotCreateCourse() {
        AuthenticatedUser student = new AuthenticatedUser(8, "student", UserRole.STUDENT);

        assertThatThrownBy(() -> service.create(student, new CreateCourseRequest("课程", null, null)))
                .isInstanceOf(BusinessException.class).hasMessage("仅教师或管理员可以创建课程");
    }

    @Test
    void nonOwnerCannotUpdateCourse() {
        AuthenticatedUser otherTeacher = new AuthenticatedUser(9, "other", UserRole.TEACHER);
        when(repository.findById(1, 9)).thenReturn(Optional.of(
                course(1, 7, CourseVisibility.PUBLIC, CourseStatus.ACTIVE, false)));

        assertThatThrownBy(() -> service.update(1, otherTeacher,
                new UpdateCourseRequest("新标题", null, null, null, null)))
                .isInstanceOf(BusinessException.class).hasMessage("无权管理该课程");
    }

    @Test
    void archivedCourseCannotBeJoined() {
        AuthenticatedUser student = new AuthenticatedUser(8, "student", UserRole.STUDENT);
        when(repository.findById(1, 8)).thenReturn(Optional.of(
                course(1, 7, CourseVisibility.PUBLIC, CourseStatus.ARCHIVED, false)));

        assertThatThrownBy(() -> service.join(1, student))
                .isInstanceOf(BusinessException.class).hasMessage("课程已归档，不能加入");
    }

    private CourseResponse course(long id, long creatorId, CourseVisibility visibility,
                                  CourseStatus status, boolean joined) {
        Instant now = Instant.parse("2026-09-19T00:00:00Z");
        return new CourseResponse(id, "课程", null, null, creatorId, visibility, status,
                0, 0, 0, new CourseResponse.Creator(creatorId, "教师"), joined, now, now);
    }
}
