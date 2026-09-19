package com.courseplatform.backend.admin;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseRepository;
import com.courseplatform.backend.course.CourseResponse;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.user.User;
import com.courseplatform.backend.user.UserRepository;
import com.courseplatform.backend.user.UserResponse;
import com.courseplatform.backend.user.UserRole;
import com.courseplatform.backend.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final CourseRepository courses;
    private final ResourceRepository resources;
    private final AdminStatisticsRepository statistics;

    public AdminService(UserRepository users, CourseRepository courses, ResourceRepository resources,
                        AdminStatisticsRepository statistics) {
        this.users = users;
        this.courses = courses;
        this.resources = resources;
        this.statistics = statistics;
    }

    public PageResult<UserResponse> users(int page, int size, String keyword, UserRole role) {
        validatePage(page, size);
        return users.findPage(page, size, normalize(keyword), role);
    }

    public UserResponse user(long userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public void updateStatus(long userId, AuthenticatedUser operator, UpdateUserStatusRequest request) {
        requireUser(userId);
        if (userId == operator.id()) {
            throw new BusinessException(40960, "不能修改自己的账号状态", HttpStatus.CONFLICT);
        }
        users.updateStatus(userId, request.status());
        if (request.status() == UserStatus.DISABLED) users.revokeRefreshSessions(userId);
    }

    @Transactional
    public void updateRole(long userId, AuthenticatedUser operator, UpdateUserRoleRequest request) {
        requireUser(userId);
        if (userId == operator.id()) {
            throw new BusinessException(40961, "不能修改自己的管理员角色", HttpStatus.CONFLICT);
        }
        users.updateRole(userId, request.role());
        users.revokeRefreshSessions(userId);
    }

    public PageResult<CourseResponse> courses(AuthenticatedUser operator, int page, int size, String keyword) {
        validatePage(page, size);
        return courses.findVisiblePage(operator.id(), true, page, size, normalize(keyword), null, null, null);
    }

    @Transactional
    public void deleteCourse(long courseId) {
        if (courses.delete(courseId) == 0) {
            throw new BusinessException(40420, "课程不存在", HttpStatus.NOT_FOUND);
        }
    }

    public PageResult<ResourceResponse> resources(AuthenticatedUser operator, int page, int size,
                                                   String keyword, Long courseId) {
        validatePage(page, size);
        return resources.findSearchPage(operator.id(), true, normalize(keyword), courseId, null,
                null, null, null, "newest", page, size);
    }

    @Transactional
    public void deleteResource(long resourceId) {
        if (resources.delete(resourceId) == 0) {
            throw new BusinessException(40440, "学习资料不存在", HttpStatus.NOT_FOUND);
        }
    }

    public AdminStatisticsResponse statistics() {
        return statistics.get();
    }

    private User requireUser(long id) {
        return users.findById(id)
                .orElseThrow(() -> new BusinessException(40410, "用户不存在", HttpStatus.NOT_FOUND));
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
