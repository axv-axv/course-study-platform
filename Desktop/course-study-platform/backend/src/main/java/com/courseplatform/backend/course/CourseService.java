package com.courseplatform.backend.course;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {
    private final CourseRepository courses;

    public CourseService(CourseRepository courses) {
        this.courses = courses;
    }

    @Transactional
    public CourseResponse create(AuthenticatedUser user, CreateCourseRequest request) {
        requireTeacher(user);
        CourseVisibility visibility = request.visibility() == null ? CourseVisibility.PUBLIC : request.visibility();
        return courses.create(user.id(), request.title().trim(), normalize(request.description()), visibility);
    }

    public PageResult<CourseResponse> list(AuthenticatedUser user, int page, int size, String keyword,
                                           Long creatorId, CourseVisibility visibility, CourseStatus status) {
        validatePage(page, size);
        return courses.findVisiblePage(user.id(), isAdmin(user), page, size, normalize(keyword), creatorId, visibility, status);
    }

    public CourseResponse detail(long id, AuthenticatedUser user) {
        CourseResponse course = requireCourse(id, user);
        requireView(course, user);
        return course;
    }

    @Transactional
    public CourseResponse update(long id, AuthenticatedUser user, UpdateCourseRequest request) {
        CourseResponse course = requireCourse(id, user);
        requireManage(course, user);
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(40011, "课程标题不能为空", HttpStatus.BAD_REQUEST);
        }
        return courses.update(id, user.id(), request);
    }

    @Transactional
    public void delete(long id, AuthenticatedUser user) {
        CourseResponse course = requireCourse(id, user);
        requireManage(course, user);
        courses.delete(id);
    }

    @Transactional
    public CourseMembershipResponse join(long id, AuthenticatedUser user) {
        CourseResponse course = requireCourse(id, user);
        if (course.status() != CourseStatus.ACTIVE) {
            throw new BusinessException(40921, "课程已归档，不能加入", HttpStatus.CONFLICT);
        }
        if (course.creatorId() == user.id()) {
            throw new BusinessException(40922, "课程创建者无需加入自己的课程", HttpStatus.CONFLICT);
        }
        return courses.join(id, user.id());
    }

    @Transactional
    public void leave(long id, AuthenticatedUser user) {
        requireCourse(id, user);
        courses.removeMember(id, user.id());
    }

    public PageResult<CourseMemberResponse> members(long id, AuthenticatedUser user, int page, int size, String keyword) {
        validatePage(page, size);
        requireManage(requireCourse(id, user), user);
        return courses.findMembers(id, page, size, normalize(keyword));
    }

    @Transactional
    public void removeMember(long id, long memberId, AuthenticatedUser user) {
        CourseResponse course = requireCourse(id, user);
        requireManage(course, user);
        if (memberId == course.creatorId()) {
            throw new BusinessException(40923, "不能移除课程创建者", HttpStatus.CONFLICT);
        }
        courses.removeMember(id, memberId);
    }

    public PageResult<CourseResponse> myJoined(AuthenticatedUser user, int page, int size) {
        validatePage(page, size);
        return courses.findJoinedPage(user.id(), page, size);
    }

    public PageResult<CourseResponse> myCreated(AuthenticatedUser user, int page, int size) {
        validatePage(page, size);
        return courses.findCreatedPage(user.id(), page, size);
    }

    public CourseResponse requireManageable(long id, AuthenticatedUser user) {
        CourseResponse course = requireCourse(id, user);
        requireManage(course, user);
        return course;
    }

    public CourseResponse requireViewable(long id, AuthenticatedUser user) {
        CourseResponse course = requireCourse(id, user);
        requireView(course, user);
        return course;
    }

    private CourseResponse requireCourse(long id, AuthenticatedUser user) {
        return courses.findById(id, user.id())
                .orElseThrow(() -> new BusinessException(40420, "课程不存在", HttpStatus.NOT_FOUND));
    }

    private void requireView(CourseResponse course, AuthenticatedUser user) {
        boolean allowed = course.visibility() == CourseVisibility.PUBLIC || course.joined()
                || course.creatorId() == user.id() || isAdmin(user);
        if (!allowed) {
            throw new BusinessException(40320, "无权查看该课程", HttpStatus.FORBIDDEN);
        }
    }

    private void requireManage(CourseResponse course, AuthenticatedUser user) {
        if (course.creatorId() != user.id() && !isAdmin(user)) {
            throw new BusinessException(40321, "无权管理该课程", HttpStatus.FORBIDDEN);
        }
    }

    private void requireTeacher(AuthenticatedUser user) {
        if (user.role() != UserRole.TEACHER && user.role() != UserRole.ADMIN) {
            throw new BusinessException(40322, "仅教师或管理员可以创建课程", HttpStatus.FORBIDDEN);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(40003, "page 必须大于 0，size 必须在 1 到 100 之间", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isAdmin(AuthenticatedUser user) {
        return user.role() == UserRole.ADMIN;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
