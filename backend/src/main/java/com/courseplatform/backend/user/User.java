package com.courseplatform.backend.user;

import java.time.Instant;

public record User(
        long id,
        String username,
        String passwordHash,
        String email,
        String nickname,
        String bio,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
