package com.courseplatform.backend.teacher;

import java.time.Instant;

public record TeacherApplication(
        long id,
        long userId,
        String username,
        String nickname,
        String reason,
        TeacherApplicationStatus status,
        String reviewComment,
        Long reviewedBy,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
