package com.courseplatform.backend.course;

import java.time.Instant;

public record CourseMembershipResponse(long courseId, long userId, Instant joinedAt) {
}
