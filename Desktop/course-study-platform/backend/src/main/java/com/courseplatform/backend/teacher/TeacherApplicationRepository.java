package com.courseplatform.backend.teacher;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TeacherApplicationRepository {
    private static final String SELECT_WITH_USER = """
            SELECT a.*, u.username, u.nickname
            FROM teacher_applications a JOIN users u ON u.id = a.user_id
            """;
    private final JdbcClient jdbcClient;

    public TeacherApplicationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public TeacherApplication create(long userId, String reason) {
        long id = jdbcClient.sql("""
                INSERT INTO teacher_applications (user_id, reason)
                VALUES (:userId, :reason) RETURNING id
                """).param("userId", userId).param("reason", reason).query(Long.class).single();
        return findById(id).orElseThrow();
    }

    public Optional<TeacherApplication> findById(long id) {
        return jdbcClient.sql(SELECT_WITH_USER + " WHERE a.id = :id")
                .param("id", id).query(this::map).optional();
    }

    public Optional<TeacherApplication> findPendingByIdForUpdate(long id) {
        return jdbcClient.sql(SELECT_WITH_USER + " WHERE a.id = :id AND a.status = 'PENDING' FOR UPDATE OF a")
                .param("id", id).query(this::map).optional();
    }

    public Optional<TeacherApplication> findLatestByUser(long userId) {
        return jdbcClient.sql(SELECT_WITH_USER + " WHERE a.user_id = :userId ORDER BY a.created_at DESC LIMIT 1")
                .param("userId", userId).query(this::map).optional();
    }

    public boolean hasPending(long userId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM teacher_applications WHERE user_id = :userId AND status = 'PENDING'")
                .param("userId", userId).query(Integer.class).single() > 0;
    }

    public List<TeacherApplication> findPage(TeacherApplicationStatus status, int offset, int size) {
        if (status == null) {
            return jdbcClient.sql(SELECT_WITH_USER + " ORDER BY a.created_at DESC OFFSET :offset LIMIT :size")
                    .param("offset", offset).param("size", size).query(this::map).list();
        }
        return jdbcClient.sql(SELECT_WITH_USER + " WHERE a.status = :status ORDER BY a.created_at DESC OFFSET :offset LIMIT :size")
                .param("status", status.name()).param("offset", offset).param("size", size).query(this::map).list();
    }

    public long count(TeacherApplicationStatus status) {
        if (status == null) {
            return jdbcClient.sql("SELECT COUNT(*) FROM teacher_applications").query(Long.class).single();
        }
        return jdbcClient.sql("SELECT COUNT(*) FROM teacher_applications WHERE status = :status")
                .param("status", status.name()).query(Long.class).single();
    }

    public void review(long id, TeacherApplicationStatus status, String comment, long reviewerId) {
        jdbcClient.sql("""
                UPDATE teacher_applications
                SET status = :status, review_comment = :comment, reviewed_by = :reviewerId,
                    reviewed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND status = 'PENDING'
                """)
                .param("status", status.name()).param("comment", comment)
                .param("reviewerId", reviewerId).param("id", id).update();
    }

    private TeacherApplication map(ResultSet rs, int rowNum) throws SQLException {
        return new TeacherApplication(
                rs.getLong("id"), rs.getLong("user_id"), rs.getString("username"), rs.getString("nickname"),
                rs.getString("reason"), TeacherApplicationStatus.valueOf(rs.getString("status")),
                rs.getString("review_comment"), nullableLong(rs, "reviewed_by"),
                nullableInstant(rs, "reviewed_at"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
