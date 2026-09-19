package com.courseplatform.backend.resource;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.ChapterRepository;
import com.courseplatform.backend.course.ChapterResponse;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.file.FileService;
import com.courseplatform.backend.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ResourceService {
    private final ResourceRepository resources;
    private final CourseService courses;
    private final ChapterRepository chapters;
    private final FileService files;

    public ResourceService(ResourceRepository resources, CourseService courses,
                           ChapterRepository chapters, FileService files) {
        this.resources = resources;
        this.courses = courses;
        this.chapters = chapters;
        this.files = files;
    }

    @Transactional
    public ResourceResponse create(AuthenticatedUser user, CreateResourceRequest request) {
        courses.requireManageable(request.courseId(), user);
        validateChapter(request.courseId(), request.chapterId());
        if (request.fileId() != null) files.requireOwned(request.fileId(), user);
        String externalUrl = validateUrl(request.externalUrl());
        validateSource(request.resourceType(), request.fileId(), externalUrl);
        return resources.create(request, user.id(), externalUrl);
    }

    @Transactional
    public ResourceResponse detail(long id, AuthenticatedUser user) {
        ResourceResponse resource = require(id);
        courses.requireViewable(resource.courseId(), user);
        resources.incrementView(id);
        return resource;
    }

    public PageResult<ResourceResponse> courseResources(long courseId, AuthenticatedUser user, int page, int size,
                                                        Long chapterId, ResourceType type, Long tagId, String keyword) {
        validatePage(page, size);
        courses.requireViewable(courseId, user);
        validateChapter(courseId, chapterId);
        return resources.findPage(courseId, chapterId, type, tagId, normalize(keyword), page, size);
    }

    public PageResult<ResourceResponse> chapterResources(long chapterId, AuthenticatedUser user, int page, int size,
                                                         ResourceType type, Long tagId, String keyword) {
        ChapterResponse chapter = chapters.findById(chapterId)
                .orElseThrow(() -> new BusinessException(40421, "章节不存在", HttpStatus.NOT_FOUND));
        return courseResources(chapter.courseId(), user, page, size, chapterId, type, tagId, keyword);
    }

    @Transactional
    public ResourceResponse update(long id, AuthenticatedUser user, UpdateResourceRequest request) {
        ResourceResponse current = require(id);
        courses.requireManageable(current.courseId(), user);
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(40041, "资料标题不能为空", HttpStatus.BAD_REQUEST);
        }
        Long chapterId = request.chapterId() == null ? current.chapterId() : request.chapterId();
        validateChapter(current.courseId(), chapterId);
        Long fileId = request.fileId() != null ? request.fileId()
                : request.externalUrl() != null ? null : current.fileId();
        String externalUrl = request.externalUrl() != null ? validateUrl(request.externalUrl())
                : request.fileId() != null ? null : current.externalUrl();
        if (fileId != null && !fileId.equals(current.fileId())) files.requireOwned(fileId, user);
        ResourceType type = request.resourceType() == null ? current.resourceType() : request.resourceType();
        validateSource(type, fileId, externalUrl);
        return resources.update(id, user.id(), request, chapterId, fileId, externalUrl);
    }

    @Transactional
    public void delete(long id, AuthenticatedUser user) {
        ResourceResponse resource = require(id);
        courses.requireManageable(resource.courseId(), user);
        resources.delete(id);
    }

    public List<TagResponse> tags(String keyword) {
        return resources.findTags(normalize(keyword));
    }

    @Transactional
    public TagResponse createTag(AuthenticatedUser user, CreateTagRequest request) {
        if (user.role() == UserRole.STUDENT) {
            throw new BusinessException(40340, "仅教师或管理员可以创建标签", HttpStatus.FORBIDDEN);
        }
        String name = request.name().trim();
        return resources.findTagByName(name).orElseGet(() -> resources.createTag(name));
    }

    @Transactional
    public void setTags(long id, AuthenticatedUser user, SetResourceTagsRequest request) {
        ResourceResponse resource = require(id);
        courses.requireManageable(resource.courseId(), user);
        List<Long> ids = new LinkedHashSet<>(request.tagIds()).stream().toList();
        if (ids.stream().anyMatch(value -> value == null || value < 1) || resources.countTags(ids) != ids.size()) {
            throw new BusinessException(40042, "tagIds 包含不存在的标签", HttpStatus.BAD_REQUEST);
        }
        resources.replaceTags(id, ids);
    }

    @Transactional
    public void removeTag(long id, long tagId, AuthenticatedUser user) {
        ResourceResponse resource = require(id);
        courses.requireManageable(resource.courseId(), user);
        resources.removeTag(id, tagId);
    }

    private ResourceResponse require(long id) {
        return resources.findById(id)
                .orElseThrow(() -> new BusinessException(40440, "学习资料不存在", HttpStatus.NOT_FOUND));
    }

    public ResourceResponse requireViewable(long id, AuthenticatedUser user) {
        ResourceResponse resource = require(id);
        courses.requireViewable(resource.courseId(), user);
        return resource;
    }

    private void validateChapter(long courseId, Long chapterId) {
        if (chapterId == null) return;
        ChapterResponse chapter = chapters.findById(chapterId)
                .orElseThrow(() -> new BusinessException(40421, "章节不存在", HttpStatus.NOT_FOUND));
        if (chapter.courseId() != courseId) {
            throw new BusinessException(40043, "章节不属于指定课程", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateSource(ResourceType type, Long fileId, String externalUrl) {
        if (fileId == null && externalUrl == null) {
            throw new BusinessException(40044, "资料必须关联文件或外部链接", HttpStatus.BAD_REQUEST);
        }
        if (type == ResourceType.LINK && externalUrl == null) {
            throw new BusinessException(40045, "链接资料必须提供外部链接", HttpStatus.BAD_REQUEST);
        }
    }

    private String validateUrl(String value) {
        String normalized = normalize(value);
        if (normalized == null) return null;
        try {
            URI uri = new URI(normalized);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw new URISyntaxException(normalized, "scheme");
            if (uri.getHost() == null) throw new URISyntaxException(normalized, "host");
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new BusinessException(40046, "外部链接必须是有效的 http/https 地址", HttpStatus.BAD_REQUEST);
        }
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
