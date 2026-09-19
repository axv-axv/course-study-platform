package com.courseplatform.backend.course;

import com.courseplatform.backend.user.UserRole;

import java.time.Instant;

public record CourseMemberResponse(
        long userId,
        String username,
        String nickname,
        String avatarUrl,
        UserRole role,
        Instant joinedAt
) {
}
