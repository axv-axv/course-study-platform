package com.courseplatform.backend.teacher;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.user.User;
import com.courseplatform.backend.user.UserRepository;
import com.courseplatform.backend.user.UserRole;
import com.courseplatform.backend.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherApplicationService {
    private final TeacherApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public TeacherApplicationService(
            TeacherApplicationRepository applicationRepository,
            UserRepository userRepository,
            UserService userService
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public TeacherApplication apply(AuthenticatedUser principal, CreateTeacherApplicationRequest request) {
        User user = userService.requireActive(principal.id());
        if (user.role() != UserRole.STUDENT) {
            throw new BusinessException(40910, "当前用户无需申请教师身份", HttpStatus.CONFLICT);
        }
        if (applicationRepository.hasPending(user.id())) {
            throw new BusinessException(40911, "已有待审核的教师申请", HttpStatus.CONFLICT);
        }
        return applicationRepository.create(user.id(), request.reason().trim());
    }

    public TeacherApplication latest(AuthenticatedUser principal) {
        return applicationRepository.findLatestByUser(principal.id())
                .orElseThrow(() -> new BusinessException(40410, "尚未提交教师申请", HttpStatus.NOT_FOUND));
    }

    public PageResult<TeacherApplication> list(TeacherApplicationStatus status, int page, int size) {
        validatePage(page, size);
        return new PageResult<>(
                applicationRepository.findPage(status, (page - 1) * size, size),
                page, size, applicationRepository.count(status)
        );
    }

    @Transactional
    public TeacherApplication approve(long id, AuthenticatedUser admin, ReviewTeacherApplicationRequest request) {
        TeacherApplication application = pending(id);
        userRepository.updateRole(application.userId(), UserRole.TEACHER);
        applicationRepository.review(id, TeacherApplicationStatus.APPROVED, normalize(request.comment()), admin.id());
        return applicationRepository.findById(id).orElseThrow();
    }

    @Transactional
    public TeacherApplication reject(long id, AuthenticatedUser admin, ReviewTeacherApplicationRequest request) {
        pending(id);
        applicationRepository.review(id, TeacherApplicationStatus.REJECTED, normalize(request.comment()), admin.id());
        return applicationRepository.findById(id).orElseThrow();
    }

    private TeacherApplication pending(long id) {
        return applicationRepository.findPendingByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(40912, "申请不存在或已处理", HttpStatus.CONFLICT));
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
