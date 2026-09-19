package com.courseplatform.backend.auth;

import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.user.User;
import com.courseplatform.backend.user.UserRepository;
import com.courseplatform.backend.user.UserResponse;
import com.courseplatform.backend.user.UserRole;
import com.courseplatform.backend.user.UserStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshDuration;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @org.springframework.beans.factory.annotation.Value("${app.security.refresh-token-days}") long refreshDays
    ) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshDuration = Duration.ofDays(refreshDays);
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = normalizeNullable(request.email());
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(40901, "用户名已存在", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(email, null)) {
            throw new BusinessException(40902, "邮箱已被使用", HttpStatus.CONFLICT);
        }
        try {
            User user = userRepository.create(username, passwordEncoder.encode(request.password()), email, UserRole.STUDENT);
            return UserResponse.from(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(40901, "用户名或邮箱已存在", HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }
        ensureActive(user);
        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String hash = hash(request.refreshToken());
        RefreshSession session = refreshSessionRepository.findByHashForUpdate(hash)
                .orElseThrow(() -> new BusinessException(40102, "Refresh Token 无效", HttpStatus.UNAUTHORIZED));
        Instant now = Instant.now();
        if (session.revokedAt() != null || !session.expiresAt().isAfter(now)) {
            throw new BusinessException(40102, "Refresh Token 已失效", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new BusinessException(40102, "Refresh Token 无效", HttpStatus.UNAUTHORIZED));
        ensureActive(user);

        String rawToken = newRefreshToken();
        UUID newId = UUID.randomUUID();
        refreshSessionRepository.create(newId, user.id(), hash(rawToken), now.plus(refreshDuration));
        refreshSessionRepository.rotate(session.id(), newId, now);
        return new TokenResponse(jwtService.createAccessToken(user), rawToken, jwtService.expiresInSeconds());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshSessionRepository.revoke(hash(request.refreshToken()), Instant.now());
    }

    private TokenResponse issueTokenPair(User user) {
        String rawRefresh = newRefreshToken();
        refreshSessionRepository.create(UUID.randomUUID(), user.id(), hash(rawRefresh), Instant.now().plus(refreshDuration));
        return new TokenResponse(jwtService.createAccessToken(user), rawRefresh, jwtService.expiresInSeconds());
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void ensureActive(User user) {
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(40301, "用户已被禁用", HttpStatus.FORBIDDEN);
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(40101, "用户名或密码错误", HttpStatus.UNAUTHORIZED);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }
}
