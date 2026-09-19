package com.courseplatform.backend.course;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class ChapterService {
    private final ChapterRepository chapters;
    private final CourseService courses;

    public ChapterService(ChapterRepository chapters, CourseService courses) {
        this.chapters = chapters;
        this.courses = courses;
    }

    @Transactional
    public ChapterResponse create(long courseId, AuthenticatedUser user, CreateChapterRequest request) {
        courses.requireManageable(courseId, user);
        return chapters.create(courseId, request.title().trim(), normalize(request.description()));
    }

    public List<ChapterResponse> list(long courseId, AuthenticatedUser user) {
        courses.requireViewable(courseId, user);
        return chapters.findByCourse(courseId);
    }

    public ChapterResponse detail(long id, AuthenticatedUser user) {
        ChapterResponse chapter = require(id);
        courses.requireViewable(chapter.courseId(), user);
        return chapter;
    }

    @Transactional
    public ChapterResponse update(long id, AuthenticatedUser user, UpdateChapterRequest request) {
        ChapterResponse chapter = require(id);
        courses.requireManageable(chapter.courseId(), user);
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(40012, "章节标题不能为空", HttpStatus.BAD_REQUEST);
        }
        String title = request.title() == null ? chapter.title() : request.title().trim();
        String description = request.description() == null ? chapter.description() : normalize(request.description());
        return chapters.update(id, title, description);
    }

    @Transactional
    public void delete(long id, AuthenticatedUser user) {
        ChapterResponse chapter = require(id);
        courses.requireManageable(chapter.courseId(), user);
        chapters.delete(id);
    }

    @Transactional
    public void reorder(long courseId, AuthenticatedUser user, ReorderChaptersRequest request) {
        courses.requireManageable(courseId, user);
        List<Long> current = chapters.findByCourse(courseId).stream().map(ChapterResponse::id).toList();
        List<Long> requested = request.chapterIds();
        if (current.size() != requested.size() || new HashSet<>(requested).size() != requested.size()
                || !new HashSet<>(current).equals(new HashSet<>(requested))) {
            throw new BusinessException(40013, "chapterIds 必须完整且不能重复", HttpStatus.BAD_REQUEST);
        }
        for (int index = 0; index < requested.size(); index++) {
            chapters.updateOrder(requested.get(index), index);
        }
    }

    private ChapterResponse require(long id) {
        return chapters.findById(id)
                .orElseThrow(() -> new BusinessException(40421, "章节不存在", HttpStatus.NOT_FOUND));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
