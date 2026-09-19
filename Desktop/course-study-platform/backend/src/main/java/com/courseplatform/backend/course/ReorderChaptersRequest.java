package com.courseplatform.backend.course;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderChaptersRequest(@NotEmpty List<Long> chapterIds) {
}
