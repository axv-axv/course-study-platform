package com.courseplatform.backend.admin;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseRepository;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.user.User;
import com.courseplatform.backend.user.UserRepository;
import com.courseplatform.backend.user.UserRole;
import com.courseplatform.backend.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final ResourceRepository resources = mock(ResourceRepository.class);
    private final AdminStatisticsRepository statistics = mock(AdminStatisticsRepository.class);
    private final AdminService service = new AdminService(users, courses, resources, statistics);
    private final AuthenticatedUser admin = new AuthenticatedUser(1, "admin", UserRole.ADMIN);

    @Test
    void disablingUserRevokesRefreshSessions() {
        when(users.findById(2)).thenReturn(Optional.of(user(2, UserRole.STUDENT)));

        service.updateStatus(2, admin, new UpdateUserStatusRequest(UserStatus.DISABLED));

        verify(users).updateStatus(2, UserStatus.DISABLED);
        verify(users).revokeRefreshSessions(2);
    }

    @Test
    void adminCannotDemoteSelf() {
        when(users.findById(1)).thenReturn(Optional.of(user(1, UserRole.ADMIN)));

        assertThatThrownBy(() -> service.updateRole(1, admin, new UpdateUserRoleRequest(UserRole.STUDENT)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不能修改自己的管理员角色");
    }

    @Test
    void deletingMissingResourceReturnsNotFound() {
        when(resources.delete(99)).thenReturn(0);

        assertThatThrownBy(() -> service.deleteResource(99))
                .isInstanceOf(BusinessException.class)
                .hasMessage("学习资料不存在");
    }

    private User user(long id, UserRole role) {
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        return new User(id, "user" + id, "hash", null, "用户", null, null,
                role, UserStatus.ACTIVE, 0, now, now);
    }
}
