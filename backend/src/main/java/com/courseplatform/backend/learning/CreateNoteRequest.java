package com.courseplatform.backend.learning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
        @NotBlank @Size(max = 2000) String content,
        @Valid LearningPosition position
) {
}
