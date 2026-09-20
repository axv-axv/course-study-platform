package com.courseplatform.backend.ai;

import java.time.Instant;
import java.util.List;

public record AiMessageResponse(
        long id,
        String role,
        String content,
        List<AiSourceResponse> sources,
        Instant createdAt
) {
}
