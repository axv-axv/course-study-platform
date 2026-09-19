package com.courseplatform.backend.resource;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SetResourceTagsRequest(@NotNull List<Long> tagIds) {
}
