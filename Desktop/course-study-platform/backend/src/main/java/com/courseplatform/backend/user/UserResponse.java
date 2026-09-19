package com.courseplatform.backend.user;

import java.time.Instant;

public record UserResponse(
        long id,
        String username,
        String nickname,
        String email,
        String avatarUrl,
        String bio,
        UserRole role,
        UserStatus status,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(), user.username(), user.nickname(), user.email(), user.avatarUrl(),
                user.bio(), user.role(), user.status(), user.createdAt()
        );
    }
}
