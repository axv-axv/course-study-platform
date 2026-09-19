package com.courseplatform.backend.teacher;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.user.User;
import com.courseplatform.backend.user.UserRepository;
import com.courseplatform.backend.user.UserRole;
import com.courseplatform.backend.user.UserService;
import com.courseplatform.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherApplicationServiceTest {
    private TeacherApplicationRepository applications;
    private UserRepository users;
    private UserService userService;
    private TeacherApplicationService service;

    @BeforeEach
    void setUp() {
        applications = mock(TeacherApplicationRepository.class);
        users = mock(UserRepository.class);
        userService = mock(UserService.class);
        service = new TeacherApplicationService(applications, users, userService);
    }

    @Test
    void studentCanSubmitApplication() {
        AuthenticatedUser principal = new AuthenticatedUser(1, "alice", UserRole.STUDENT);
        User student = user(UserRole.STUDENT);
        TeacherApplication created = application(1, TeacherApplicationStatus.PENDING);
        when(userService.requireActive(1)).thenReturn(student);
        when(applications.create(1, "我希望创建和管理课程资料。" )).thenReturn(created);

        TeacherApplication result = service.apply(principal,
                new CreateTeacherApplicationRequest("我希望创建和管理课程资料。"));

        assertThat(result.status()).isEqualTo(TeacherApplicationStatus.PENDING);
    }

    @Test
    void duplicatePendingApplicationIsRejected() {
        AuthenticatedUser principal = new AuthenticatedUser(1, "alice", UserRole.STUDENT);
        when(userService.requireActive(1)).thenReturn(user(UserRole.STUDENT));
        when(applications.hasPending(1)).thenReturn(true);

        assertThatThrownBy(() -> service.apply(principal,
                new CreateTeacherApplicationRequest("我希望创建和管理课程资料。")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已有待审核的教师申请");
    }

    @Test
    void administratorApprovalPromotesUser() {
        AuthenticatedUser admin = new AuthenticatedUser(99, "admin", UserRole.ADMIN);
        TeacherApplication pending = application(7, TeacherApplicationStatus.PENDING);
        TeacherApplication approved = application(7, TeacherApplicationStatus.APPROVED);
        when(applications.findPendingByIdForUpdate(7)).thenReturn(Optional.of(pending));
        when(applications.findById(7)).thenReturn(Optional.of(approved));

        TeacherApplication result = service.approve(7, admin, new ReviewTeacherApplicationRequest("材料通过"));

        assertThat(result.status()).isEqualTo(TeacherApplicationStatus.APPROVED);
        verify(users).updateRole(1, UserRole.TEACHER);
        verify(applications).review(7, TeacherApplicationStatus.APPROVED, "材料通过", 99);
    }

    private User user(UserRole role) {
        return new User(1, "alice", "hash", "alice@example.com", "Alice", null, null,
                role, UserStatus.ACTIVE, 0, Instant.now(), Instant.now());
    }

    private TeacherApplication application(long id, TeacherApplicationStatus status) {
        return new TeacherApplication(id, 1, "alice", "Alice", "申请教师权限的理由文本", status,
                null, null, null, Instant.now(), Instant.now());
    }
}
