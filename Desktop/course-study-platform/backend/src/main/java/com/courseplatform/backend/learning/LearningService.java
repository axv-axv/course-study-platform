package com.courseplatform.backend.learning;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.resource.ResourceService;
import com.courseplatform.backend.resource.ResourceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LearningService {
    private final LearningRepository learning;
    private final ResourceService resources;
    private final CourseService courses;

    public LearningService(LearningRepository learning, ResourceService resources, CourseService courses) {
        this.learning = learning;
        this.resources = resources;
        this.courses = courses;
    }

    @Transactional
    public void favorite(long resourceId, AuthenticatedUser user) {
        resources.requireViewable(resourceId, user);
        learning.favorite(user.id(), resourceId);
    }

    @Transactional
    public void unfavorite(long resourceId, AuthenticatedUser user) {
        learning.unfavorite(user.id(), resourceId);
    }

    public boolean favoriteStatus(long resourceId, AuthenticatedUser user) {
        resources.requireViewable(resourceId, user);
        return learning.isFavorite(user.id(), resourceId);
    }

    public PageResult<FavoriteResponse> favorites(AuthenticatedUser user, int page, int size, Long courseId,
                                                   ResourceType type, String keyword) {
        validatePage(page, size);
        return learning.findFavorites(user.id(), page, size, courseId, type, normalize(keyword));
    }

    @Transactional
    public ProgressResponse updateProgress(long resourceId, AuthenticatedUser user, UpdateProgressRequest request) {
        resources.requireViewable(resourceId, user);
        StudyStatus expected = request.progress() == 0 ? StudyStatus.NOT_STARTED
                : request.progress() == 100 ? StudyStatus.COMPLETED : StudyStatus.IN_PROGRESS;
        if (request.status() != expected) {
            throw new BusinessException(40050, "学习状态与进度百分比不一致", HttpStatus.BAD_REQUEST);
        }
        return learning.upsertProgress(user.id(), resourceId, request);
    }

    public ProgressResponse progress(long resourceId, AuthenticatedUser user) {
        resources.requireViewable(resourceId, user);
        return learning.findProgress(user.id(), resourceId).orElseGet(ProgressResponse::notStarted);
    }

    public CourseProgressResponse courseProgress(long courseId, AuthenticatedUser user) {
        courses.requireViewable(courseId, user);
        return learning.courseProgress(user.id(), courseId);
    }

    @Transactional
    public NoteResponse createNote(long resourceId, AuthenticatedUser user, CreateNoteRequest request) {
        resources.requireViewable(resourceId, user);
        return learning.createNote(user.id(), resourceId,
                new CreateNoteRequest(request.content().trim(), request.position()));
    }

    public List<NoteResponse> resourceNotes(long resourceId, AuthenticatedUser user) {
        resources.requireViewable(resourceId, user);
        return learning.findResourceNotes(user.id(), resourceId);
    }

    public PageResult<NoteResponse> notes(AuthenticatedUser user, int page, int size, Long courseId,
                                           Long resourceId, String keyword) {
        validatePage(page, size);
        return learning.findNotes(user.id(), page, size, courseId, resourceId, normalize(keyword));
    }

    @Transactional
    public NoteResponse updateNote(long id, AuthenticatedUser user, UpdateNoteRequest request) {
        NoteResponse note = requireOwnedNote(id, user);
        resources.requireViewable(note.resourceId(), user);
        return learning.updateNote(id, request.content().trim());
    }

    @Transactional
    public void deleteNote(long id, AuthenticatedUser user) {
        requireOwnedNote(id, user);
        learning.deleteNote(id);
    }

    public List<RecentLearningResponse> recent(AuthenticatedUser user) {
        return learning.recent(user.id(), 20);
    }

    public LearningDashboardResponse dashboard(AuthenticatedUser user) {
        return learning.dashboard(user.id());
    }

    private NoteResponse requireOwnedNote(long id, AuthenticatedUser user) {
        NoteResponse note = learning.findNote(id)
                .orElseThrow(() -> new BusinessException(40450, "笔记不存在", HttpStatus.NOT_FOUND));
        if (note.userId() != user.id()) {
            throw new BusinessException(40350, "无权操作该笔记", HttpStatus.FORBIDDEN);
        }
        return note;
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(40003, "page 必须大于 0，size 必须在 1 到 100 之间", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
