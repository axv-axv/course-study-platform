package com.courseplatform.backend.file;

import java.time.Instant;

public record StoredFile(
        long id,
        String objectKey,
        String originalName,
        String contentType,
        long size,
        String sha256,
        long uploaderId,
        Instant createdAt
) {
}
