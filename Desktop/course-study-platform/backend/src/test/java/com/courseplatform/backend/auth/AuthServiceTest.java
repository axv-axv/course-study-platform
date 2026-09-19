package com.courseplatform.backend.auth;

import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.user.User;
import com.courseplatform.backend.user.UserRepository;
import com.courseplatform.backend.user.UserRole;
import com.courseplatform.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private UserRepository users;
    private RefreshSessionRepository sessions;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        sessions = mock(RefreshSessionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        service = new AuthService(users, sessions, passwordEncoder, jwtService, 7);
    }

    @Test
    void publicRegistrationAlwaysCreatesStudent() {
        User created = user(1, "alice", UserRole.STUDENT);
        when(users.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("hash");
        when(users.create("alice", "hash", "alice@example.com", UserRole.STUDENT)).thenReturn(created);

        var response = service.register(new RegisterRequest("alice", "Password123", "Alice@Example.com"));

        assertThat(response.role()).isEqualTo(UserRole.STUDENT);
        verify(users).create("alice", "hash", "alice@example.com", UserRole.STUDENT);
    }

    @Test
    void loginReturnsRotatableTokenPair() {
        User user = user(1, "alice", UserRole.STUDENT);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", user.passwordHash())).thenReturn(true);
        when(jwtService.createAccessToken(user)).thenReturn("access-token");
        when(jwtService.expiresInSeconds()).thenReturn(900L);

        TokenResponse response = service.login(new LoginRequest("alice", "Password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(900);
        verify(sessions).create(any(), eq(1L), any(), any());
    }

    @Test
    void invalidPasswordDoesNotRevealWhichCredentialFailed() {
        User user = user(1, "alice", UserRole.STUDENT);
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    private User user(long id, String username, UserRole role) {
        return new User(id, username, "password-hash", "alice@example.com", username, null, null,
                role, UserStatus.ACTIVE, 0, Instant.now(), Instant.now());
    }
}
