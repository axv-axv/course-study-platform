package com.courseplatform.backend.resource;

import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.file.FileInfoResponse;
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
public class ResourceRepository {
    private static final String SELECT = """
            SELECT r.*, c.title AS course_title, ch.title AS chapter_title, u.nickname AS creator_nickname,
                   f.original_name AS file_name, f.content_type AS file_content_type,
                   f.size_bytes AS file_size, f.sha256 AS file_sha256, f.created_at AS file_created_at
            FROM resources r
            JOIN courses c ON c.id = r.course_id
            LEFT JOIN chapters ch ON ch.id = r.chapter_id
            JOIN users u ON u.id = r.creator_id
            LEFT JOIN files f ON f.id = r.file_id
            """;

    private final JdbcClient jdbcClient;

    public ResourceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ResourceResponse create(CreateResourceRequest request, long creatorId, String externalUrl) {
        long id = jdbcClient.sql("""
                INSERT INTO resources(course_id, chapter_id, title, description, resource_type, file_id, external_url, creator_id)
                VALUES (:courseId, :chapterId, :title, :description, :type, :fileId, :externalUrl, :creatorId)
                RETURNING id
                """).param("courseId", request.courseId()).param("chapterId", request.chapterId())
                .param("title", request.title().trim()).param("description", normalize(request.description()))
                .param("type", request.resourceType().name()).param("fileId", request.fileId())
                .param("externalUrl", externalUrl).param("creatorId", creatorId)
                .query(Long.class).single();
        return findById(id).orElseThrow();
    }

    public Optional<ResourceResponse> findById(long id) {
        return jdbcClient.sql(SELECT + " WHERE r.id = :id").param("id", id)
                .query(this::map).optional().map(this::withTags);
    }

    public PageResult<ResourceResponse> findPage(long courseId, Long chapterId, ResourceType type,
                                                  Long tagId, String keyword, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE r.course_id = :courseId");
        Map<String, Object> params = new HashMap<>();
        params.put("courseId", courseId);
        if (chapterId != null) { where.append(" AND r.chapter_id = :chapterId"); params.put("chapterId", chapterId); }
        if (type != null) { where.append(" AND r.resource_type = :type"); params.put("type", type.name()); }
        if (tagId != null) {
            where.append(" AND EXISTS(SELECT 1 FROM resource_tags rt WHERE rt.resource_id = r.id AND rt.tag_id = :tagId)");
            params.put("tagId", tagId);
        }
        if (keyword != null) {
            where.append(" AND (LOWER(r.title) LIKE :keyword OR LOWER(COALESCE(r.description, '')) LIKE :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        List<ResourceResponse> items = jdbcClient.sql(SELECT + where + " ORDER BY r.created_at DESC, r.id DESC LIMIT :limit OFFSET :offset")
                .params(params).param("limit", size).param("offset", (page - 1) * size)
                .query(this::map).list().stream().map(this::withTags).toList();
        long total = jdbcClient.sql("SELECT COUNT(*) FROM resources r" + where).params(params).query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    public PageResult<ResourceResponse> findSearchPage(long viewerId, boolean admin, String keyword,
                                                        Long courseId, Long chapterId, ResourceType type,
                                                        Long tagId, Long creatorId, String sort,
                                                        int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (!admin) {
            where.append(" AND c.status = 'ACTIVE' AND (c.visibility = 'PUBLIC' OR c.creator_id = :viewerId OR EXISTS")
                    .append("(SELECT 1 FROM course_members cm WHERE cm.course_id = c.id AND cm.user_id = :viewerId))");
            params.put("viewerId", viewerId);
        }
        if (keyword != null) {
            where.append(" AND (LOWER(r.title) LIKE :keyword OR LOWER(COALESCE(r.description, '')) LIKE :keyword OR LOWER(c.title) LIKE :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (courseId != null) { where.append(" AND r.course_id = :courseId"); params.put("courseId", courseId); }
        if (chapterId != null) { where.append(" AND r.chapter_id = :chapterId"); params.put("chapterId", chapterId); }
        if (type != null) { where.append(" AND r.resource_type = :type"); params.put("type", type.name()); }
        if (creatorId != null) { where.append(" AND r.creator_id = :creatorId"); params.put("creatorId", creatorId); }
        if (tagId != null) {
            where.append(" AND EXISTS(SELECT 1 FROM resource_tags rt WHERE rt.resource_id = r.id AND rt.tag_id = :tagId)");
            params.put("tagId", tagId);
        }
        String orderBy = switch (sort) {
            case "popular" -> "r.view_count DESC, r.id DESC";
            case "downloads" -> "r.download_count DESC, r.id DESC";
            case "title" -> "LOWER(r.title), r.id";
            default -> "r.created_at DESC, r.id DESC";
        };
        List<ResourceResponse> items = jdbcClient.sql(SELECT + where + " ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset")
                .params(params).param("limit", size).param("offset", (page - 1) * size)
                .query(this::map).list().stream().map(this::withTags).toList();
        long total = jdbcClient.sql("SELECT COUNT(*) FROM resources r JOIN courses c ON c.id = r.course_id" + where)
                .params(params).query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    public PageResult<TagResponse> findTagsPage(String keyword, int page, int size) {
        String where = keyword == null ? "" : " WHERE LOWER(name) LIKE :keyword";
        JdbcClient.StatementSpec itemsSpec = jdbcClient.sql("SELECT * FROM tags" + where
                + " ORDER BY name, id LIMIT :limit OFFSET :offset");
        JdbcClient.StatementSpec countSpec = jdbcClient.sql("SELECT COUNT(*) FROM tags" + where);
        if (keyword != null) {
            String value = "%" + keyword.toLowerCase() + "%";
            itemsSpec = itemsSpec.param("keyword", value);
            countSpec = countSpec.param("keyword", value);
        }
        List<TagResponse> items = itemsSpec.param("limit", size).param("offset", (page - 1) * size)
                .query(this::mapTag).list();
        return new PageResult<>(items, page, size, countSpec.query(Long.class).single());
    }

    public ResourceResponse update(long id, long viewerId, UpdateResourceRequest request,
                                   Long chapterId, Long fileId, String externalUrl) {
        ResourceResponse current = findById(id).orElseThrow();
        jdbcClient.sql("""
                UPDATE resources SET chapter_id = :chapterId, title = :title, description = :description,
                    resource_type = :type, file_id = :fileId, external_url = :externalUrl,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id
                """).param("chapterId", chapterId)
                .param("title", request.title() == null ? current.title() : request.title().trim())
                .param("description", request.description() == null ? current.description() : normalize(request.description()))
                .param("type", (request.resourceType() == null ? current.resourceType() : request.resourceType()).name())
                .param("fileId", fileId).param("externalUrl", externalUrl).param("id", id).update();
        return findById(id).orElseThrow();
    }

    public int delete(long id) {
        return jdbcClient.sql("DELETE FROM resources WHERE id = :id").param("id", id).update();
    }

    public void incrementView(long id) {
        jdbcClient.sql("UPDATE resources SET view_count = view_count + 1 WHERE id = :id").param("id", id).update();
    }

    public List<TagResponse> findTags(String keyword) {
        String where = keyword == null ? "" : " WHERE LOWER(name) LIKE :keyword";
        JdbcClient.StatementSpec spec = jdbcClient.sql("SELECT * FROM tags" + where + " ORDER BY name, id");
        if (keyword != null) spec = spec.param("keyword", "%" + keyword.toLowerCase() + "%");
        return spec.query(this::mapTag).list();
    }

    public Optional<TagResponse> findTagByName(String name) {
        return jdbcClient.sql("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name)")
                .param("name", name).query(this::mapTag).optional();
    }

    public TagResponse createTag(String name) {
        long id = jdbcClient.sql("INSERT INTO tags(name) VALUES (:name) RETURNING id")
                .param("name", name).query(Long.class).single();
        return jdbcClient.sql("SELECT * FROM tags WHERE id = :id").param("id", id).query(this::mapTag).single();
    }

    public long countTags(List<Long> tagIds) {
        if (tagIds.isEmpty()) return 0;
        return jdbcClient.sql("SELECT COUNT(*) FROM tags WHERE id IN (:ids)").param("ids", tagIds).query(Long.class).single();
    }

    public void replaceTags(long resourceId, List<Long> tagIds) {
        jdbcClient.sql("DELETE FROM resource_tags WHERE resource_id = :resourceId").param("resourceId", resourceId).update();
        for (Long tagId : tagIds) {
            jdbcClient.sql("INSERT INTO resource_tags(resource_id, tag_id) VALUES (:resourceId, :tagId)")
                    .param("resourceId", resourceId).param("tagId", tagId).update();
        }
    }

    public void removeTag(long resourceId, long tagId) {
        jdbcClient.sql("DELETE FROM resource_tags WHERE resource_id = :resourceId AND tag_id = :tagId")
                .param("resourceId", resourceId).param("tagId", tagId).update();
    }

    private ResourceResponse withTags(ResourceResponse value) {
        List<TagResponse> tags = jdbcClient.sql("""
                SELECT t.* FROM tags t JOIN resource_tags rt ON rt.tag_id = t.id
                WHERE rt.resource_id = :resourceId ORDER BY t.name, t.id
                """).param("resourceId", value.id()).query(this::mapTag).list();
        return new ResourceResponse(value.id(), value.courseId(), value.chapterId(), value.title(), value.description(),
                value.resourceType(), value.fileId(), value.externalUrl(), value.creatorId(), value.viewCount(),
                value.downloadCount(), value.aiIndexStatus(), value.aiIndexError(), value.indexedAt(), value.createdAt(),
                value.updatedAt(), value.course(), value.chapter(), value.file(), tags, false, null, value.creator());
    }

    private ResourceResponse map(ResultSet rs, int rowNum) throws SQLException {
        Long chapterId = nullableLong(rs, "chapter_id");
        Long fileId = nullableLong(rs, "file_id");
        FileInfoResponse file = fileId == null ? null : new FileInfoResponse(fileId, rs.getString("file_name"),
                rs.getString("file_name"), rs.getString("file_content_type"), rs.getLong("file_size"),
                "/api/v1/files/" + fileId + "/preview", rs.getString("file_sha256"),
                rs.getObject("file_created_at", OffsetDateTime.class).toInstant());
        OffsetDateTime indexedAt = rs.getObject("indexed_at", OffsetDateTime.class);
        return new ResourceResponse(rs.getLong("id"), rs.getLong("course_id"), chapterId, rs.getString("title"),
                rs.getString("description"), ResourceType.valueOf(rs.getString("resource_type")), fileId,
                rs.getString("external_url"), rs.getLong("creator_id"), rs.getLong("view_count"),
                rs.getLong("download_count"), AiIndexStatus.valueOf(rs.getString("ai_index_status")),
                rs.getString("ai_index_error"), indexedAt == null ? null : indexedAt.toInstant(),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant(),
                new ResourceResponse.NamedRef(rs.getLong("course_id"), rs.getString("course_title")),
                chapterId == null ? null : new ResourceResponse.NamedRef(chapterId, rs.getString("chapter_title")),
                file, List.of(), false, null,
                new ResourceResponse.Creator(rs.getLong("creator_id"), rs.getString("creator_nickname")));
    }

    private TagResponse mapTag(ResultSet rs, int rowNum) throws SQLException {
        return new TagResponse(rs.getLong("id"), rs.getString("name"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
