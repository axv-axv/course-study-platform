package com.courseplatform.backend.ai;

import java.util.List;

public record AiChatResponse(long conversationId, String answer, List<AiSourceResponse> sources) {
}
