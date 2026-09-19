package com.courseplatform.backend.resource;

import java.time.Instant;

public record TagResponse(long id, String name, Instant createdAt) {
}
