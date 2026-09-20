package com.courseplatform.backend.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAiConversationRequest(
        @NotNull Long courseId,
        @Size(max = 100) String title
) {
}
