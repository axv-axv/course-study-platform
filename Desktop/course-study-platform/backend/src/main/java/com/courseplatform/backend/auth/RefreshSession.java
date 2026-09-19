package com.courseplatform.backend.auth;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
        UUID id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt
) {
}
