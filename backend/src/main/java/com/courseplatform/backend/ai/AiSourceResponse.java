package com.courseplatform.backend.ai;

public record AiSourceResponse(
        long resourceId,
        String resourceTitle,
        Long chapterId,
        String chapterTitle,
        Integer page,
        String snippet
) {
}
