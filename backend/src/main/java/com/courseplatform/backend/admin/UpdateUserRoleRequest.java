package com.courseplatform.backend.admin;

import com.courseplatform.backend.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull UserRole role) {
}
