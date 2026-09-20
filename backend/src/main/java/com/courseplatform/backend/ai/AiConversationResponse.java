package com.courseplatform.backend.ai;

import java.time.Instant;
import java.util.List;

public record AiConversationResponse(
        long id,
        long courseId,
        String title,
        List<AiMessageResponse> messages,
        Instant createdAt,
        Instant updatedAt
) {
}
