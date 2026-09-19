package com.courseplatform.backend.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50) String nickname,
        @Email @Size(max = 254) String email,
        @Size(max = 500) String bio
) {
}
