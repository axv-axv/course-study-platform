package com.courseplatform.backend.rag;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.resource.AiIndexStatus;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.resource.ResourceType;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagIndexServiceTest {
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final CourseService courses = mock(CourseService.class);
    private final RagIndexRepository indexes = mock(RagIndexRepository.class);
    private final RagIndexService service = new RagIndexService(resources, courses, indexes);
    private final AuthenticatedUser teacher = new AuthenticatedUser(7, "teacher", UserRole.TEACHER);

    @Test
    void queuesSupportedFileForIndexing() {
        ResourceResponse original = resource(ResourceType.PDF, 9L, AiIndexStatus.NOT_INDEXED);
        ResourceResponse processing = resource(ResourceType.PDF, 9L, AiIndexStatus.PROCESSING);
        when(resources.findById(1)).thenReturn(Optional.of(original)).thenReturn(Optional.of(processing));

        AiIndexStatusResponse response = service.reindex(1, teacher);

        verify(courses).requireManageable(11, teacher);
        verify(indexes).enqueue(1);
        assertThat(response.status()).isEqualTo(AiIndexStatus.PROCESSING);
    }

    @Test
    void processingResourceIsNotQueuedTwice() {
        ResourceResponse processing = resource(ResourceType.TXT, 9L, AiIndexStatus.PROCESSING);
        when(resources.findById(1)).thenReturn(Optional.of(processing));

        service.reindex(1, teacher);

        verify(indexes, never()).enqueue(1);
    }

    @Test
    void rejectsUnsupportedVideoResource() {
        when(resources.findById(1)).thenReturn(Optional.of(resource(ResourceType.VIDEO, 9L, AiIndexStatus.NOT_INDEXED)));

        assertThatThrownBy(() -> service.reindex(1, teacher))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该资料类型暂不支持建立 AI 索引");
    }

    @Test
    void deletingIndexChecksManagePermission() {
        when(resources.findById(1)).thenReturn(Optional.of(resource(ResourceType.PDF, 9L, AiIndexStatus.INDEXED)));

        service.delete(1, teacher);

        verify(courses).requireManageable(11, teacher);
        verify(indexes).deleteIndex(1);
    }

    private ResourceResponse resource(ResourceType type, Long fileId, AiIndexStatus status) {
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        return new ResourceResponse(1, 11, 12L, "资料", null, type, fileId, null,
                7, 0, 0, status, null, null, now, now,
                new ResourceResponse.NamedRef(11, "课程"), new ResourceResponse.NamedRef(12, "章节"),
                null, List.of(), false, null, new ResourceResponse.Creator(7, "教师"));
    }
}
