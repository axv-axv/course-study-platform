package com.courseplatform.backend.teacher;

import jakarta.validation.constraints.Size;

public record ReviewTeacherApplicationRequest(@Size(max = 1000) String comment) {
}
