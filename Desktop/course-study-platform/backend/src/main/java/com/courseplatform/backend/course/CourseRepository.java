package com.courseplatform.backend.course;

import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.user.UserRole;
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
public class CourseRepository {
    private static final String COURSE_SELECT = """
            SELECT c.*,
                   u.nickname AS creator_nickname,
                   (SELECT COUNT(*) FROM course_members cm WHERE cm.course_id = c.id) AS member_count,
                   (SELECT COUNT(*) FROM chapters ch WHERE ch.course_id = c.id) AS chapter_count,
                   (SELECT COUNT(*) FROM resources r WHERE r.course_id = c.id) AS resource_count,
                   EXISTS(SELECT 1 FROM course_members cm WHERE cm.course_id = c.id AND cm.user_id = :viewerId) AS joined
            FROM courses c
            JOIN users u ON u.id = c.creator_id
            """;

    private final JdbcClient jdbcClient;

    public CourseRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public CourseResponse create(long creatorId, String title, String description, CourseVisibility visibility) {
        long id = jdbcClient.sql("""
                INSERT INTO courses(title, description, creator_id, visibility)
                VALUES (:title, :description, :creatorId, :visibility)
                RETURNING id
                """)
                .param("title", title).param("description", description)
                .param("creatorId", creatorId).param("visibility", visibility.name())
                .query(Long.class).single();
        return findById(id, creatorId).orElseThrow();
    }

    public Optional<CourseResponse> findById(long id, long viewerId) {
        return jdbcClient.sql(COURSE_SELECT + " WHERE c.id = :id")
                .param("id", id).param("viewerId", viewerId)
                .query(this::mapCourse).optional();
    }

    public PageResult<CourseResponse> findVisiblePage(
            long viewerId, boolean admin, int page, int size, String keyword,
            Long creatorId, CourseVisibility visibility, CourseStatus status
    ) {
        Filter filter = visibleFilter(viewerId, admin, keyword, creatorId, visibility, status);
        List<CourseResponse> items = jdbcClient.sql(COURSE_SELECT + filter.where()
                        + " ORDER BY c.created_at DESC, c.id DESC LIMIT :limit OFFSET :offset")
                .params(filter.params()).param("viewerId", viewerId)
                .param("limit", size).param("offset", (page - 1) * size)
                .query(this::mapCourse).list();
        long total = jdbcClient.sql("SELECT COUNT(*) FROM courses c " + filter.where())
                .params(filter.params()).param("viewerId", viewerId)
                .query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    public PageResult<CourseResponse> findCreatedPage(long userId, int page, int size) {
        return simplePage("c.creator_id = :ownerId", Map.of("ownerId", userId), userId, page, size);
    }

    public PageResult<CourseResponse> findJoinedPage(long userId, int page, int size) {
        return simplePage("EXISTS(SELECT 1 FROM course_members mine WHERE mine.course_id = c.id AND mine.user_id = :ownerId)",
                Map.of("ownerId", userId), userId, page, size);
    }

    public CourseResponse update(long id, long viewerId, UpdateCourseRequest request) {
        CourseResponse current = findById(id, viewerId).orElseThrow();
        String title = request.title() == null ? current.title() : request.title().trim();
        String description = request.description() == null ? current.description() : normalize(request.description());
        String coverUrl = request.coverUrl() == null ? current.coverUrl() : normalize(request.coverUrl());
        CourseVisibility visibility = request.visibility() == null ? current.visibility() : request.visibility();
        CourseStatus status = request.status() == null ? current.status() : request.status();
        jdbcClient.sql("""
                UPDATE courses SET title = :title, description = :description, cover_url = :coverUrl,
                    visibility = :visibility, status = :status, version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """)
                .param("title", title).param("description", description).param("coverUrl", coverUrl)
                .param("visibility", visibility.name()).param("status", status.name()).param("id", id).update();
        return findById(id, viewerId).orElseThrow();
    }

    public int delete(long id) {
        return jdbcClient.sql("DELETE FROM courses WHERE id = :id").param("id", id).update();
    }

    public CourseMembershipResponse join(long courseId, long userId) {
        jdbcClient.sql("""
                INSERT INTO course_members(course_id, user_id) VALUES (:courseId, :userId)
                ON CONFLICT (course_id, user_id) DO NOTHING
                """).param("courseId", courseId).param("userId", userId).update();
        return jdbcClient.sql("SELECT course_id, user_id, joined_at FROM course_members WHERE course_id = :courseId AND user_id = :userId")
                .param("courseId", courseId).param("userId", userId)
                .query((rs, rowNum) -> new CourseMembershipResponse(
                        rs.getLong("course_id"), rs.getLong("user_id"),
                        rs.getObject("joined_at", OffsetDateTime.class).toInstant())).single();
    }

    public int removeMember(long courseId, long userId) {
        return jdbcClient.sql("DELETE FROM course_members WHERE course_id = :courseId AND user_id = :userId")
                .param("courseId", courseId).param("userId", userId).update();
    }

    public PageResult<CourseMemberResponse> findMembers(long courseId, int page, int size, String keyword) {
        String predicate = keyword == null ? "" : " AND (LOWER(u.username) LIKE :keyword OR LOWER(COALESCE(u.nickname, '')) LIKE :keyword)";
        Map<String, Object> params = new HashMap<>();
        params.put("courseId", courseId);
        if (keyword != null) params.put("keyword", "%" + keyword.toLowerCase() + "%");
        List<CourseMemberResponse> items = jdbcClient.sql("""
                SELECT cm.user_id, u.username, u.nickname, u.avatar_url, u.role, cm.joined_at
                FROM course_members cm JOIN users u ON u.id = cm.user_id
                WHERE cm.course_id = :courseId
                """ + predicate + " ORDER BY cm.joined_at DESC, cm.user_id DESC LIMIT :limit OFFSET :offset")
                .params(params).param("limit", size).param("offset", (page - 1) * size)
                .query(this::mapMember).list();
        long total = jdbcClient.sql("SELECT COUNT(*) FROM course_members cm JOIN users u ON u.id = cm.user_id WHERE cm.course_id = :courseId" + predicate)
                .params(params).query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    private PageResult<CourseResponse> simplePage(String predicate, Map<String, Object> params, long viewerId, int page, int size) {
        String where = " WHERE " + predicate;
        List<CourseResponse> items = jdbcClient.sql(COURSE_SELECT + where + " ORDER BY c.created_at DESC, c.id DESC LIMIT :limit OFFSET :offset")
                .params(params).param("viewerId", viewerId).param("limit", size).param("offset", (page - 1) * size)
                .query(this::mapCourse).list();
        long total = jdbcClient.sql("SELECT COUNT(*) FROM courses c" + where).params(params).query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    private Filter visibleFilter(long viewerId, boolean admin, String keyword, Long creatorId,
                                 CourseVisibility visibility, CourseStatus status) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (!admin) {
            where.append(" AND (c.visibility = 'PUBLIC' OR c.creator_id = :viewerId OR EXISTS")
                    .append("(SELECT 1 FROM course_members access_member WHERE access_member.course_id = c.id AND access_member.user_id = :viewerId))");
        }
        if (keyword != null) {
            where.append(" AND (LOWER(c.title) LIKE :keyword OR LOWER(COALESCE(c.description, '')) LIKE :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (creatorId != null) {
            where.append(" AND c.creator_id = :creatorId");
            params.put("creatorId", creatorId);
        }
        if (visibility != null) {
            where.append(" AND c.visibility = :visibility");
            params.put("visibility", visibility.name());
        }
        if (status != null) {
            where.append(" AND c.status = :status");
            params.put("status", status.name());
        }
        return new Filter(where.toString(), params);
    }

    private CourseResponse mapCourse(ResultSet rs, int rowNum) throws SQLException {
        return new CourseResponse(
                rs.getLong("id"), rs.getString("title"), rs.getString("description"), rs.getString("cover_url"),
                rs.getLong("creator_id"), CourseVisibility.valueOf(rs.getString("visibility")),
                CourseStatus.valueOf(rs.getString("status")), rs.getLong("member_count"),
                rs.getLong("resource_count"), rs.getLong("chapter_count"),
                new CourseResponse.Creator(rs.getLong("creator_id"), rs.getString("creator_nickname")),
                rs.getBoolean("joined"), rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private CourseMemberResponse mapMember(ResultSet rs, int rowNum) throws SQLException {
        return new CourseMemberResponse(rs.getLong("user_id"), rs.getString("username"), rs.getString("nickname"),
                rs.getString("avatar_url"), UserRole.valueOf(rs.getString("role")),
                rs.getObject("joined_at", OffsetDateTime.class).toInstant());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Filter(String where, Map<String, Object> params) {
    }
}
