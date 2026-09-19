package com.courseplatform.backend.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResourceRequest(
        @NotNull Long courseId,
        Long chapterId,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 500) String description,
        @NotNull ResourceType resourceType,
        Long fileId,
        @Size(max = 2000) String externalUrl
) {
}
