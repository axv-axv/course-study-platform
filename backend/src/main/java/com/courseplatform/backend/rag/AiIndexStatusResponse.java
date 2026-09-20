package com.courseplatform.backend.rag;

import com.courseplatform.backend.resource.AiIndexStatus;

import java.time.Instant;

public record AiIndexStatusResponse(
        long resourceId,
        AiIndexStatus status,
        Instant indexedAt,
        String error
) {
}
