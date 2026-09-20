package com.courseplatform.backend.rag;

import com.courseplatform.backend.resource.AiIndexStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RagIndexRepository {
    private final JdbcClient jdbcClient;

    public RagIndexRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void enqueue(long resourceId) {
        jdbcClient.sql("""
                UPDATE resources
                SET ai_index_status = 'PROCESSING', ai_index_error = NULL, indexed_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :resourceId
                """).param("resourceId", resourceId).update();
        jdbcClient.sql("""
                INSERT INTO rag_index_jobs(resource_id, status)
                VALUES (:resourceId, 'PENDING')
                ON CONFLICT DO NOTHING
                """).param("resourceId", resourceId).update();
    }

    public void deleteIndex(long resourceId) {
        jdbcClient.sql("DELETE FROM rag_index_jobs WHERE resource_id = :resourceId")
                .param("resourceId", resourceId).update();
        jdbcClient.sql("DELETE FROM rag_chunks WHERE resource_id = :resourceId")
                .param("resourceId", resourceId).update();
        jdbcClient.sql("""
                UPDATE resources
                SET ai_index_status = 'NOT_INDEXED', ai_index_error = NULL, indexed_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :resourceId
                """).param("resourceId", resourceId).update();
    }
}
