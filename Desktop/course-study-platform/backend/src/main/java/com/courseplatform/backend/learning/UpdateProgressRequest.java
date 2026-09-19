package com.courseplatform.backend.learning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateProgressRequest(
        @NotNull StudyStatus status,
        @Min(0) @Max(100) int progress,
        @Valid LearningPosition position
) {
}
