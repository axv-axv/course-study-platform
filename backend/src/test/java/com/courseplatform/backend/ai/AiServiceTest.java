package com.courseplatform.backend.ai;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.ChapterRepository;
import com.courseplatform.backend.course.ChapterResponse;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.resource.ResourceType;
import com.courseplatform.backend.resource.AiIndexStatus;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceTest {
    private final AiConversationRepository conversations = mock(AiConversationRepository.class);
    private final CourseService courses = mock(CourseService.class);
    private final ChapterRepository chapters = mock(ChapterRepository.class);
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final AiWorkerClient worker = mock(AiWorkerClient.class);
    private final AiService service = new AiService(conversations, courses, chapters, resources, worker);
    private final AuthenticatedUser student = new AuthenticatedUser(8, "student", UserRole.STUDENT);

    @Test
    void createsConversationAndChecksCourseAccess() {
        when(conversations.create(8, 11, "复习")).thenReturn(conversation(3, 11, "复习"));

        CreateAiConversationResponse response = service.create(student, new CreateAiConversationRequest(11L, " 复习 "));

        verify(courses).requireViewable(11, student);
        assertThat(response.conversationId()).isEqualTo(3);
    }

    @Test
    void sendsQuestionToWorkerAndPersistsBothMessages() {
        when(conversations.create(8, 11, "什么是梯度下降？"))
                .thenReturn(conversation(3, 11, "什么是梯度下降？"));
        AiSourceResponse source = new AiSourceResponse(21, "机器学习", 12L, "优化", 3, "相关内容");
        when(worker.chat(11, 12L, 21L, "什么是梯度下降？"))
                .thenReturn(new AiWorkerClient.WorkerChatResponse("回答", List.of(source)));
        when(chapters.findById(12)).thenReturn(Optional.of(chapter()));
        when(resources.findById(21)).thenReturn(Optional.of(resource()));

        AiChatResponse response = service.chat(student,
                new AiChatRequest(null, 11L, 12L, 21L, " 什么是梯度下降？ "));

        verify(conversations).addMessage(3, "USER", "什么是梯度下降？", List.of());
        verify(conversations).addMessage(3, "ASSISTANT", "回答", List.of(source));
        assertThat(response.sources()).containsExactly(source);
    }

    @Test
    void rejectsConversationFromAnotherCourse() {
        when(conversations.findByIdAndUser(3, 8)).thenReturn(Optional.of(conversation(3, 99, "旧对话")));

        assertThatThrownBy(() -> service.chat(student, new AiChatRequest(3L, 11L, null, null, "问题")))
                .isInstanceOf(BusinessException.class).hasMessage("对话不属于指定课程");
    }

    @Test
    void rejectsResourceOutsideRequestedChapter() {
        when(chapters.findById(12)).thenReturn(Optional.of(chapter()));
        ResourceResponse resource = resource();
        ResourceResponse otherChapter = new ResourceResponse(resource.id(), resource.courseId(), 13L, resource.title(),
                resource.description(), resource.resourceType(), resource.fileId(), resource.externalUrl(),
                resource.creatorId(), resource.viewCount(), resource.downloadCount(), resource.aiIndexStatus(),
                resource.aiIndexError(), resource.indexedAt(), resource.createdAt(), resource.updatedAt(), resource.course(),
                new ResourceResponse.NamedRef(13, "其他章节"), resource.file(), resource.tags(), resource.favorite(),
                resource.progress(), resource.creator());
        when(resources.findById(21)).thenReturn(Optional.of(otherChapter));

        assertThatThrownBy(() -> service.chat(student, new AiChatRequest(null, 11L, 12L, 21L, "问题")))
                .isInstanceOf(BusinessException.class).hasMessage("学习资料不属于指定章节");
    }

    private AiConversationResponse conversation(long id, long courseId, String title) {
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        return new AiConversationResponse(id, courseId, title, List.of(), now, now);
    }

    private ChapterResponse chapter() {
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        return new ChapterResponse(12, 11, "优化", null, 0, 1, now, now);
    }

    private ResourceResponse resource() {
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        return new ResourceResponse(21, 11, 12L, "机器学习", null, ResourceType.PDF, 9L, null,
                7, 0, 0, AiIndexStatus.INDEXED, null, now, now, now,
                new ResourceResponse.NamedRef(11, "课程"), new ResourceResponse.NamedRef(12, "优化"),
                null, List.of(), false, null, new ResourceResponse.Creator(7, "教师"));
    }
}
