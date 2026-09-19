package com.courseplatform.backend.search;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseRepository;
import com.courseplatform.backend.course.CourseResponse;
import com.courseplatform.backend.course.CourseStatus;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.resource.ResourceType;
import com.courseplatform.backend.resource.TagResponse;
import com.courseplatform.backend.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
    private final CourseRepository courses;
    private final ResourceRepository resources;

    public SearchService(CourseRepository courses, ResourceRepository resources) {
        this.courses = courses;
        this.resources = resources;
    }

    public UnifiedSearchResponse unified(AuthenticatedUser user, String keyword, String type, int page, int size) {
        String normalized = requireKeyword(keyword);
        validatePage(page, size);
        String normalizedType = type == null || type.isBlank() ? null : type.trim().toLowerCase();
        if (normalizedType != null && !normalizedType.matches("course|resource|tag")) {
            throw new BusinessException(40051, "type 仅支持 course、resource 或 tag", HttpStatus.BAD_REQUEST);
        }
        PageResult<CourseResponse> coursePage = normalizedType == null || normalizedType.equals("course")
                ? courses.findVisiblePage(user.id(), isAdmin(user), page, size, normalized, null, null, CourseStatus.ACTIVE) : null;
        PageResult<ResourceResponse> resourcePage = normalizedType == null || normalizedType.equals("resource")
                ? resources.findSearchPage(user.id(), isAdmin(user), normalized, null, null, null, null, null,
                "newest", page, size) : null;
        PageResult<TagResponse> tagPage = normalizedType == null || normalizedType.equals("tag")
                ? resources.findTagsPage(normalized, page, size) : null;
        return new UnifiedSearchResponse(coursePage, resourcePage, tagPage);
    }

    public PageResult<CourseResponse> courses(AuthenticatedUser user, String keyword, int page, int size) {
        validatePage(page, size);
        return courses.findVisiblePage(user.id(), isAdmin(user), page, size, requireKeyword(keyword),
                null, null, CourseStatus.ACTIVE);
    }

    public PageResult<ResourceResponse> resources(AuthenticatedUser user, String keyword, Long courseId,
                                                   Long chapterId, ResourceType type, Long tagId, Long creatorId,
                                                   String sort, int page, int size) {
        validatePage(page, size);
        String normalizedSort = sort == null || sort.isBlank() ? "newest" : sort.trim().toLowerCase();
        if (!normalizedSort.matches("newest|popular|downloads|title")) {
            throw new BusinessException(40052, "sort 仅支持 newest、popular、downloads 或 title", HttpStatus.BAD_REQUEST);
        }
        return resources.findSearchPage(user.id(), isAdmin(user), normalize(keyword), courseId, chapterId,
                type, tagId, creatorId, normalizedSort, page, size);
    }

    private boolean isAdmin(AuthenticatedUser user) {
        return user.role() == UserRole.ADMIN;
    }

    private String requireKeyword(String value) {
        String normalized = normalize(value);
        if (normalized == null) throw new BusinessException(40050, "keyword 不能为空", HttpStatus.BAD_REQUEST);
        return normalized;
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
