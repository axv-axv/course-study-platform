package com.courseplatform.backend.resource;

import jakarta.validation.constraints.Size;

public record UpdateResourceRequest(
        Long chapterId,
        @Size(min = 1, max = 120) String title,
        @Size(max = 500) String description,
        ResourceType resourceType,
        Long fileId,
        @Size(max = 2000) String externalUrl
) {
}
