package com.courseplatform.backend.file;

import java.time.Instant;

public record FileInfoResponse(
        long fileId,
        String fileName,
        String name,
        String contentType,
        long size,
        String url,
        String sha256,
        Instant createdAt
) {
    public static FileInfoResponse from(StoredFile file) {
        return new FileInfoResponse(file.id(), file.originalName(), file.originalName(), file.contentType(), file.size(),
                "/api/v1/files/" + file.id() + "/preview", file.sha256(), file.createdAt());
    }
}
