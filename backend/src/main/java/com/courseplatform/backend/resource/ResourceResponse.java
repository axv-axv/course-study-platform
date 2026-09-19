package com.courseplatform.backend.resource;

import com.courseplatform.backend.file.FileInfoResponse;

import java.time.Instant;
import java.util.List;

public record ResourceResponse(
        long id,
        long courseId,
        Long chapterId,
        String title,
        String description,
        ResourceType resourceType,
        Long fileId,
        String externalUrl,
        long creatorId,
        long viewCount,
        long downloadCount,
        AiIndexStatus aiIndexStatus,
        String aiIndexError,
        Instant indexedAt,
        Instant createdAt,
        Instant updatedAt,
        NamedRef course,
        NamedRef chapter,
        FileInfoResponse file,
        List<TagResponse> tags,
        boolean favorite,
        Object progress,
        Creator creator
) {
    public record NamedRef(long id, String title) {}
    public record Creator(long id, String nickname) {}
}
