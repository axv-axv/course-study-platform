package com.courseplatform.backend.course;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ChapterRepository {
    private static final String SELECT = """
            SELECT ch.*, (SELECT COUNT(*) FROM resources r WHERE r.chapter_id = ch.id) AS resource_count FROM chapters ch
            """;

    private final JdbcClient jdbcClient;

    public ChapterRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ChapterResponse create(long courseId, String title, String description) {
        long id = jdbcClient.sql("""
                INSERT INTO chapters(course_id, title, description, sort_order)
                VALUES (:courseId, :title, :description,
                        COALESCE((SELECT MAX(sort_order) + 1 FROM chapters WHERE course_id = :courseId), 0))
                RETURNING id
                """).param("courseId", courseId).param("title", title).param("description", description)
                .query(Long.class).single();
        return findById(id).orElseThrow();
    }

    public Optional<ChapterResponse> findById(long id) {
        return jdbcClient.sql(SELECT + " WHERE ch.id = :id").param("id", id).query(this::map).optional();
    }

    public List<ChapterResponse> findByCourse(long courseId) {
        return jdbcClient.sql(SELECT + " WHERE ch.course_id = :courseId ORDER BY ch.sort_order, ch.id")
                .param("courseId", courseId).query(this::map).list();
    }

    public ChapterResponse update(long id, String title, String description) {
        jdbcClient.sql("""
                UPDATE chapters SET title = :title, description = :description, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """).param("title", title).param("description", description).param("id", id).update();
        return findById(id).orElseThrow();
    }

    public int delete(long id) {
        return jdbcClient.sql("DELETE FROM chapters WHERE id = :id").param("id", id).update();
    }

    public void updateOrder(long id, int sortOrder) {
        jdbcClient.sql("UPDATE chapters SET sort_order = :sortOrder, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
                .param("sortOrder", sortOrder).param("id", id).update();
    }

    private ChapterResponse map(ResultSet rs, int rowNum) throws SQLException {
        return new ChapterResponse(rs.getLong("id"), rs.getLong("course_id"), rs.getString("title"),
                rs.getString("description"), rs.getInt("sort_order"), rs.getLong("resource_count"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
