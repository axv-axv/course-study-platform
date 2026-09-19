package com.courseplatform.backend.admin;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AdminStatisticsRepository {
    private final JdbcClient jdbcClient;

    public AdminStatisticsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public AdminStatisticsResponse get() {
        return jdbcClient.sql("""
                SELECT
                    (SELECT COUNT(*) FROM users) AS user_count,
                    (SELECT COUNT(*) FROM courses) AS course_count,
                    (SELECT COUNT(*) FROM resources) AS resource_count,
                    (SELECT COUNT(*) FROM files) AS file_count,
                    (SELECT COUNT(*) FROM resources WHERE ai_index_status = 'INDEXED') AS indexed_resource_count,
                    (SELECT COALESCE(SUM(size_bytes), 0) FROM files) AS storage_usage
                """).query((rs, rowNum) -> new AdminStatisticsResponse(
                        rs.getLong("user_count"), rs.getLong("course_count"),
                        rs.getLong("resource_count"), rs.getLong("file_count"),
                        rs.getLong("indexed_resource_count"), rs.getLong("storage_usage")
                )).single();
    }
}
