package com.courseplatform.backend.ai;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.ChapterRepository;
import com.courseplatform.backend.course.ChapterResponse;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.resource.ResourceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {
    private final AiConversationRepository conversations;
    private final CourseService courses;
    private final ChapterRepository chapters;
    private final ResourceRepository resources;
    private final AiWorkerClient worker;

    public AiService(AiConversationRepository conversations, CourseService courses, ChapterRepository chapters,
                     ResourceRepository resources, AiWorkerClient worker) {
        this.conversations = conversations;
        this.courses = courses;
        this.chapters = chapters;
        this.resources = resources;
        this.worker = worker;
    }

    public CreateAiConversationResponse create(AuthenticatedUser user, CreateAiConversationRequest request) {
        courses.requireViewable(request.courseId(), user);
        String title = normalizeTitle(request.title(), "新对话");
        AiConversationResponse created = conversations.create(user.id(), request.courseId(), title);
        return new CreateAiConversationResponse(created.id(), created.courseId(), created.title());
    }

    public PageResult<AiConversationResponse> list(AuthenticatedUser user, Long courseId, int page, int size) {
        validatePage(page, size);
        if (courseId != null) courses.requireViewable(courseId, user);
        return conversations.findPage(user.id(), courseId, page, size);
    }

    public AiConversationResponse detail(long id, AuthenticatedUser user) {
        return requireConversation(id, user);
    }

    public void delete(long id, AuthenticatedUser user) {
        if (conversations.delete(id, user.id()) == 0) {
            throw notFound();
        }
    }

    public AiChatResponse chat(AuthenticatedUser user, AiChatRequest request) {
        courses.requireViewable(request.courseId(), user);
        validateScope(request);
        AiConversationResponse conversation;
        if (request.conversationId() == null) {
            conversation = conversations.create(user.id(), request.courseId(),
                    normalizeTitle(null, request.message()));
        } else {
            conversation = requireConversation(request.conversationId(), user);
            if (conversation.courseId() != request.courseId()) {
                throw new BusinessException(40072, "对话不属于指定课程", HttpStatus.BAD_REQUEST);
            }
        }
        String question = request.message().trim();
        conversations.addMessage(conversation.id(), "USER", question, List.of());
        AiWorkerClient.WorkerChatResponse result = worker.chat(
                request.courseId(), request.chapterId(), request.resourceId(), question);
        List<AiSourceResponse> sources = result.sources() == null ? List.of() : result.sources();
        conversations.addMessage(conversation.id(), "ASSISTANT", result.answer(), sources);
        return new AiChatResponse(conversation.id(), result.answer(), sources);
    }

    private void validateScope(AiChatRequest request) {
        if (request.chapterId() != null) {
            ChapterResponse chapter = chapters.findById(request.chapterId())
                    .orElseThrow(() -> new BusinessException(40430, "章节不存在", HttpStatus.NOT_FOUND));
            if (chapter.courseId() != request.courseId()) {
                throw new BusinessException(40073, "章节不属于指定课程", HttpStatus.BAD_REQUEST);
            }
        }
        if (request.resourceId() != null) {
            ResourceResponse resource = resources.findById(request.resourceId())
                    .orElseThrow(() -> new BusinessException(40440, "学习资料不存在", HttpStatus.NOT_FOUND));
            if (resource.courseId() != request.courseId()) {
                throw new BusinessException(40074, "学习资料不属于指定课程", HttpStatus.BAD_REQUEST);
            }
            if (request.chapterId() != null && !request.chapterId().equals(resource.chapterId())) {
                throw new BusinessException(40075, "学习资料不属于指定章节", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private AiConversationResponse requireConversation(long id, AuthenticatedUser user) {
        return conversations.findByIdAndUser(id, user.id()).orElseThrow(this::notFound);
    }

    private BusinessException notFound() {
        return new BusinessException(40470, "AI 对话不存在", HttpStatus.NOT_FOUND);
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(40003, "page 必须大于 0，size 必须在 1 到 100 之间", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeTitle(String requested, String fallback) {
        String value = requested == null || requested.isBlank() ? fallback.trim() : requested.trim();
        return value.length() <= 100 ? value : value.substring(0, 100);
    }
}
