package com.courseplatform.backend.admin;

import com.courseplatform.backend.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
