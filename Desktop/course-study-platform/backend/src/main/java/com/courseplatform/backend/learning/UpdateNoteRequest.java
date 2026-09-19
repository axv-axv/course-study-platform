package com.courseplatform.backend.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(@NotBlank @Size(max = 2000) String content) {
}
