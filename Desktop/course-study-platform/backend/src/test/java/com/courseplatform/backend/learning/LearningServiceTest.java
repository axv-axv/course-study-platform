package com.courseplatform.backend.learning;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.resource.ResourceService;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningServiceTest {
    private final LearningRepository learning = mock(LearningRepository.class);
    private final ResourceService resources = mock(ResourceService.class);
    private final CourseService courses = mock(CourseService.class);
    private final LearningService service = new LearningService(learning, resources, courses);
    private final AuthenticatedUser student = new AuthenticatedUser(8, "student", UserRole.STUDENT);

    @Test
    void rejectsProgressStatusMismatch() {
        UpdateProgressRequest request = new UpdateProgressRequest(StudyStatus.COMPLETED, 80, null);

        assertThatThrownBy(() -> service.updateProgress(3, student, request))
                .isInstanceOf(BusinessException.class).hasMessage("学习状态与进度百分比不一致");
    }

    @Test
    void favoriteIsIdempotentlyDelegated() {
        service.favorite(3, student);

        verify(resources).requireViewable(3, student);
        verify(learning).favorite(8, 3);
    }

    @Test
    void userCannotEditAnotherUsersNote() {
        Instant now = Instant.parse("2026-09-19T00:00:00Z");
        when(learning.findNote(5)).thenReturn(Optional.of(
                new NoteResponse(5, 99, 3, "内容", null, now, now, null)));

        assertThatThrownBy(() -> service.updateNote(5, student, new UpdateNoteRequest("修改")))
                .isInstanceOf(BusinessException.class).hasMessage("无权操作该笔记");
    }

    @Test
    void rejectsOversizedPage() {
        assertThatThrownBy(() -> service.notes(student, 1, 101, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("page 必须大于 0，size 必须在 1 到 100 之间");
    }
}
