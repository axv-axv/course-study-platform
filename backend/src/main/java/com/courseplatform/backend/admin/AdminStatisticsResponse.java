package com.courseplatform.backend.admin;

public record AdminStatisticsResponse(
        long userCount,
        long courseCount,
        long resourceCount,
        long fileCount,
        long indexedResourceCount,
        long storageUsage
) {
}
