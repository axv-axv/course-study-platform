package com.courseplatform.backend.auth;

import com.courseplatform.backend.user.UserRole;

public record AuthenticatedUser(long id, String username, UserRole role) {
}
