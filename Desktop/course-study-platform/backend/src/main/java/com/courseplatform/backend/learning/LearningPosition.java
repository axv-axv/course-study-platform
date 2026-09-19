package com.courseplatform.backend.learning;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record LearningPosition(
        @Min(1) Integer page,
        @DecimalMin("0.0") Double seconds
) {
}
