package com.courseplatform.backend.learning;

import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.resource.ResourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class LearningRepository {
    private static final String ACCESS = """
            (c.visibility = 'PUBLIC' OR c.creator_id = :userId OR EXISTS(
                SELECT 1 FROM course_members access_member
                WHERE access_member.course_id = c.id AND access_member.user_id = :userId))
            """;

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public LearningRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public void favorite(long userId, long resourceId) {
        jdbcClient.sql("""
                INSERT INTO favorites(user_id, resource_id) VALUES (:userId, :resourceId)
                ON CONFLICT (user_id, resource_id) DO NOTHING
                """).param("userId", userId).param("resourceId", resourceId).update();
    }

    public void unfavorite(long userId, long resourceId) {
        jdbcClient.sql("DELETE FROM favorites WHERE user_id = :userId AND resource_id = :resourceId")
                .param("userId", userId).param("resourceId", resourceId).update();
    }

    public boolean isFavorite(long userId, long resourceId) {
        return jdbcClient.sql("SELECT EXISTS(SELECT 1 FROM favorites WHERE user_id = :userId AND resource_id = :resourceId)")
                .param("userId", userId).param("resourceId", resourceId).query(Boolean.class).single();
    }

    public PageResult<FavoriteResponse> findFavorites(long userId, int page, int size, Long courseId,
                                                       ResourceType type, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE f.user_id = :userId AND ").append(ACCESS);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        if (courseId != null) { where.append(" AND r.course_id = :courseId"); params.put("courseId", courseId); }
        if (type != null) { where.append(" AND r.resource_type = :type"); params.put("type", type.name()); }
        if (keyword != null) {
            where.append(" AND (LOWER(r.title) LIKE :keyword OR LOWER(COALESCE(r.description, '')) LIKE :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        String from = " FROM favorites f JOIN resources r ON r.id = f.resource_id JOIN courses c ON c.id = r.course_id";
        List<FavoriteResponse> items = jdbcClient.sql("""
                SELECT r.id, r.title, r.description, r.resource_type, c.id AS course_id, c.title AS course_title,
                       COALESCE(lp.progress, 0) AS progress
                """ + from + " LEFT JOIN learning_progress lp ON lp.resource_id = r.id AND lp.user_id = :userId"
                        + where + " ORDER BY f.created_at DESC, r.id DESC LIMIT :limit OFFSET :offset")
                .params(params).param("limit", size).param("offset", (page - 1) * size)
                .query((rs, rowNum) -> new FavoriteResponse(rs.getLong("id"), rs.getString("title"),
                        rs.getString("description"), ResourceType.valueOf(rs.getString("resource_type")),
                        new FavoriteResponse.CourseRef(rs.getLong("course_id"), rs.getString("course_title")),
                        rs.getString("course_title"), rs.getInt("progress"))).list();
        long total = jdbcClient.sql("SELECT COUNT(*)" + from + where).params(params).query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    public ProgressResponse upsertProgress(long userId, long resourceId, UpdateProgressRequest request) {
        jdbcClient.sql("""
                INSERT INTO learning_progress(user_id, resource_id, status, progress, position)
                VALUES (:userId, :resourceId, :status, :progress, CAST(:position AS jsonb))
                ON CONFLICT (user_id, resource_id) DO UPDATE SET
                    status = EXCLUDED.status, progress = EXCLUDED.progress, position = EXCLUDED.position,
                    last_study_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                """).param("userId", userId).param("resourceId", resourceId)
                .param("status", request.status().name()).param("progress", request.progress())
                .param("position", json(request.position())).update();
        return findProgress(userId, resourceId).orElseThrow();
    }

    public Optional<ProgressResponse> findProgress(long userId, long resourceId) {
        return jdbcClient.sql("SELECT * FROM learning_progress WHERE user_id = :userId AND resource_id = :resourceId")
                .param("userId", userId).param("resourceId", resourceId)
                .query((rs, rowNum) -> new ProgressResponse(StudyStatus.valueOf(rs.getString("status")),
                        rs.getInt("progress"), position(rs.getString("position")),
                        rs.getObject("last_study_at", OffsetDateTime.class).toInstant())).optional();
    }

    public NoteResponse createNote(long userId, long resourceId, CreateNoteRequest request) {
        long id = jdbcClient.sql("""
                INSERT INTO notes(user_id, resource_id, content, position)
                VALUES (:userId, :resourceId, :content, CAST(:position AS jsonb)) RETURNING id
                """).param("userId", userId).param("resourceId", resourceId)
                .param("content", request.content().trim()).param("position", json(request.position()))
                .query(Long.class).single();
        return findNote(id).orElseThrow();
    }

    public Optional<NoteResponse> findNote(long id) {
        return jdbcClient.sql(noteSelect() + " WHERE n.id = :id").param("id", id).query(this::mapNote).optional();
    }

    public List<NoteResponse> findResourceNotes(long userId, long resourceId) {
        return jdbcClient.sql(noteSelect() + " WHERE n.user_id = :userId AND n.resource_id = :resourceId ORDER BY n.updated_at DESC, n.id DESC")
                .param("userId", userId).param("resourceId", resourceId).query(this::mapNote).list();
    }

    public PageResult<NoteResponse> findNotes(long userId, int page, int size, Long courseId,
                                               Long resourceId, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE n.user_id = :userId AND ").append(ACCESS);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        if (courseId != null) { where.append(" AND r.course_id = :courseId"); params.put("courseId", courseId); }
        if (resourceId != null) { where.append(" AND r.id = :resourceId"); params.put("resourceId", resourceId); }
        if (keyword != null) { where.append(" AND LOWER(n.content) LIKE :keyword"); params.put("keyword", "%" + keyword.toLowerCase() + "%"); }
        List<NoteResponse> items = jdbcClient.sql(noteSelect() + where + " ORDER BY n.updated_at DESC, n.id DESC LIMIT :limit OFFSET :offset")
                .params(params).param("limit", size).param("offset", (page - 1) * size).query(this::mapNote).list();
        long total = jdbcClient.sql("SELECT COUNT(*) FROM notes n JOIN resources r ON r.id = n.resource_id JOIN courses c ON c.id = r.course_id" + where)
                .params(params).query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    public NoteResponse updateNote(long id, String content) {
        jdbcClient.sql("UPDATE notes SET content = :content, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
                .param("content", content).param("id", id).update();
        return findNote(id).orElseThrow();
    }

    public void deleteNote(long id) {
        jdbcClient.sql("DELETE FROM notes WHERE id = :id").param("id", id).update();
    }

    public List<RecentLearningResponse> recent(long userId, int limit) {
        return jdbcClient.sql("""
                SELECT r.id AS resource_id, r.title, lp.progress, c.id AS course_id, c.title AS course_title,
                       r.resource_type, lp.last_study_at
                FROM learning_progress lp JOIN resources r ON r.id = lp.resource_id
                JOIN courses c ON c.id = r.course_id
                WHERE lp.user_id = :userId AND
                """ + ACCESS + " ORDER BY lp.last_study_at DESC, r.id DESC LIMIT :limit")
                .param("userId", userId).param("limit", limit).query(this::mapRecent).list();
    }

    public CourseProgressResponse courseProgress(long userId, long courseId) {
        return jdbcClient.sql("""
                SELECT COUNT(r.id) AS resource_count,
                       COUNT(r.id) FILTER (WHERE lp.status = 'COMPLETED') AS completed_count,
                       COUNT(r.id) FILTER (WHERE lp.status = 'IN_PROGRESS') AS in_progress_count,
                       COALESCE(ROUND(AVG(COALESCE(lp.progress, 0))), 0) AS progress
                FROM resources r LEFT JOIN learning_progress lp
                  ON lp.resource_id = r.id AND lp.user_id = :userId
                WHERE r.course_id = :courseId
                """).param("userId", userId).param("courseId", courseId)
                .query((rs, rowNum) -> {
                    long total = rs.getLong("resource_count");
                    long completed = rs.getLong("completed_count");
                    long inProgress = rs.getLong("in_progress_count");
                    return new CourseProgressResponse(total, completed, inProgress,
                            total - completed - inProgress, rs.getInt("progress"));
                }).single();
    }

    public LearningDashboardResponse dashboard(long userId) {
        long courseCount = scalar("SELECT COUNT(*) FROM course_members WHERE user_id = :userId", userId);
        long completed = scalar("SELECT COUNT(*) FROM learning_progress WHERE user_id = :userId AND status = 'COMPLETED'", userId);
        long favoriteCount = scalar("SELECT COUNT(*) FROM favorites WHERE user_id = :userId", userId);
        long noteCount = scalar("SELECT COUNT(*) FROM notes WHERE user_id = :userId", userId);
        List<LearningDashboardResponse.CourseSummary> courseSummaries = jdbcClient.sql("""
                SELECT c.id, c.title, c.cover_url,
                       COALESCE(ROUND(AVG(COALESCE(lp.progress, 0))), 0) AS progress
                FROM course_members cm JOIN courses c ON c.id = cm.course_id
                LEFT JOIN resources r ON r.course_id = c.id
                LEFT JOIN learning_progress lp ON lp.resource_id = r.id AND lp.user_id = :userId
                WHERE cm.user_id = :userId
                GROUP BY c.id, c.title, c.cover_url, cm.joined_at
                ORDER BY cm.joined_at DESC, c.id DESC
                """).param("userId", userId).query((rs, rowNum) -> new LearningDashboardResponse.CourseSummary(
                        rs.getLong("id"), rs.getString("title"), rs.getInt("progress"), rs.getString("cover_url"))).list();
        return new LearningDashboardResponse(courseCount, completed, favoriteCount, noteCount,
                recent(userId, 10), courseSummaries);
    }

    private long scalar(String sql, long userId) {
        return jdbcClient.sql(sql).param("userId", userId).query(Long.class).single();
    }

    private String noteSelect() {
        return """
                SELECT n.*, r.title AS resource_title, r.resource_type, c.title AS course_title
                FROM notes n JOIN resources r ON r.id = n.resource_id
                JOIN courses c ON c.id = r.course_id
                """;
    }

    private NoteResponse mapNote(ResultSet rs, int rowNum) throws SQLException {
        return new NoteResponse(rs.getLong("id"), rs.getLong("user_id"), rs.getLong("resource_id"),
                rs.getString("content"), position(rs.getString("position")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                new NoteResponse.ResourceRef(rs.getLong("resource_id"), rs.getString("resource_title"),
                        ResourceType.valueOf(rs.getString("resource_type")), rs.getString("course_title")));
    }

    private RecentLearningResponse mapRecent(ResultSet rs, int rowNum) throws SQLException {
        return new RecentLearningResponse(rs.getLong("resource_id"), rs.getString("title"), rs.getInt("progress"),
                rs.getLong("course_id"), rs.getString("course_title"), ResourceType.valueOf(rs.getString("resource_type")),
                rs.getObject("last_study_at", OffsetDateTime.class).toInstant());
    }

    private String json(LearningPosition position) {
        if (position == null) return null;
        try {
            return objectMapper.writeValueAsString(position);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("学习位置格式错误", exception);
        }
    }

    private LearningPosition position(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, LearningPosition.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据库中的学习位置格式错误", exception);
        }
    }
}
