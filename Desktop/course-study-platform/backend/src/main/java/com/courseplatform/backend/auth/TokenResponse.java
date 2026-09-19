package com.courseplatform.backend.auth;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
