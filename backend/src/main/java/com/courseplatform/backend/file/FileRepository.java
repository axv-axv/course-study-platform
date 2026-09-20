package com.courseplatform.backend.file;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class FileRepository {
    private final JdbcClient jdbcClient;

    public FileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public StoredFile create(String objectKey, String name, String contentType, long size, String sha256, long uploaderId) {
        long id = jdbcClient.sql("""
                INSERT INTO files(object_key, original_name, content_type, size_bytes, sha256, uploader_id)
                VALUES (:objectKey, :name, :contentType, :size, :sha256, :uploaderId) RETURNING id
                """).param("objectKey", objectKey).param("name", name).param("contentType", contentType)
                .param("size", size).param("sha256", sha256).param("uploaderId", uploaderId)
                .query(Long.class).single();
        return findById(id).orElseThrow();
    }

    public Optional<StoredFile> findById(long id) {
        return jdbcClient.sql("SELECT * FROM files WHERE id = :id AND status = 'AVAILABLE'")
                .param("id", id).query(this::map).optional();
    }

    public List<Long> findReferencedCourseIds(long fileId) {
        return jdbcClient.sql("SELECT DISTINCT course_id FROM resources WHERE file_id = :fileId")
                .param("fileId", fileId).query(Long.class).list();
    }

    public boolean isReferenced(long fileId) {
        return jdbcClient.sql("""
                SELECT EXISTS(SELECT 1 FROM resources WHERE file_id = :fileId)
                    OR EXISTS(SELECT 1 FROM users WHERE avatar_url = '/api/v1/public/avatars/' || CAST(:fileId AS VARCHAR))
                """)
                .param("fileId", fileId).query(Boolean.class).single();
    }

    public boolean isAvatar(long fileId) {
        return jdbcClient.sql("""
                SELECT EXISTS(SELECT 1 FROM users
                              WHERE avatar_url = '/api/v1/public/avatars/' || CAST(:fileId AS VARCHAR))
                """).param("fileId", fileId).query(Boolean.class).single();
    }

    public void incrementResourceDownloads(long fileId) {
        jdbcClient.sql("""
                UPDATE resources SET download_count = download_count + 1, updated_at = CURRENT_TIMESTAMP
                WHERE file_id = :fileId
                """).param("fileId", fileId).update();
    }

    public int delete(long id) {
        return jdbcClient.sql("DELETE FROM files WHERE id = :id").param("id", id).update();
    }

    private StoredFile map(ResultSet rs, int rowNum) throws SQLException {
        return new StoredFile(rs.getLong("id"), rs.getString("object_key"), rs.getString("original_name"),
                rs.getString("content_type"), rs.getLong("size_bytes"), rs.getString("sha256"),
                rs.getLong("uploader_id"), rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
