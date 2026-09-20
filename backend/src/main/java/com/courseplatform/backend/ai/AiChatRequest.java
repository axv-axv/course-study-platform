package com.courseplatform.backend.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        Long conversationId,
        @NotNull Long courseId,
        Long chapterId,
        Long resourceId,
        @NotBlank @Size(max = 2000) String message
) {
}
