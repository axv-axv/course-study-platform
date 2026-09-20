package com.courseplatform.backend.ai;

import com.courseplatform.backend.common.api.PageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AiConversationRepository {
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public AiConversationRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public AiConversationResponse create(long userId, long courseId, String title) {
        long id = jdbcClient.sql("""
                INSERT INTO ai_conversations(user_id, course_id, title)
                VALUES (:userId, :courseId, :title) RETURNING id
                """).param("userId", userId).param("courseId", courseId).param("title", title)
                .query(Long.class).single();
        return findByIdAndUser(id, userId).orElseThrow();
    }

    public Optional<AiConversationResponse> findByIdAndUser(long id, long userId) {
        return jdbcClient.sql("SELECT * FROM ai_conversations WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId).query(this::mapConversation).optional()
                .map(value -> withMessages(value, findMessages(value.id())));
    }

    public PageResult<AiConversationResponse> findPage(long userId, Long courseId, int page, int size) {
        String courseFilter = courseId == null ? "" : " AND course_id = :courseId";
        var query = jdbcClient.sql("SELECT * FROM ai_conversations WHERE user_id = :userId" + courseFilter
                + " ORDER BY updated_at DESC, id DESC LIMIT :limit OFFSET :offset")
                .param("userId", userId).param("limit", size).param("offset", (page - 1) * size);
        var count = jdbcClient.sql("SELECT COUNT(*) FROM ai_conversations WHERE user_id = :userId" + courseFilter)
                .param("userId", userId);
        if (courseId != null) {
            query = query.param("courseId", courseId);
            count = count.param("courseId", courseId);
        }
        List<AiConversationResponse> items = query.query(this::mapConversation).list();
        return new PageResult<>(items, page, size, count.query(Long.class).single());
    }

    public int delete(long id, long userId) {
        return jdbcClient.sql("DELETE FROM ai_conversations WHERE id = :id AND user_id = :userId")
                .param("id", id).param("userId", userId).update();
    }

    public AiMessageResponse addMessage(long conversationId, String role, String content,
                                        List<AiSourceResponse> sources) {
        String sourceJson;
        try {
            sourceJson = objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化 AI 来源", exception);
        }
        long id = jdbcClient.sql("""
                INSERT INTO ai_messages(conversation_id, role, content, sources)
                VALUES (:conversationId, :role, :content, CAST(:sources AS jsonb)) RETURNING id
                """).param("conversationId", conversationId).param("role", role)
                .param("content", content).param("sources", sourceJson).query(Long.class).single();
        jdbcClient.sql("UPDATE ai_conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = :id")
                .param("id", conversationId).update();
        return jdbcClient.sql("SELECT * FROM ai_messages WHERE id = :id").param("id", id)
                .query(this::mapMessage).single();
    }

    private List<AiMessageResponse> findMessages(long conversationId) {
        return jdbcClient.sql("SELECT * FROM ai_messages WHERE conversation_id = :conversationId ORDER BY created_at, id")
                .param("conversationId", conversationId).query(this::mapMessage).list();
    }

    private AiConversationResponse mapConversation(ResultSet rs, int rowNum) throws SQLException {
        return new AiConversationResponse(rs.getLong("id"), rs.getLong("course_id"), rs.getString("title"),
                List.of(), rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    private AiMessageResponse mapMessage(ResultSet rs, int rowNum) throws SQLException {
        try {
            List<AiSourceResponse> sources = objectMapper.readValue(rs.getString("sources"), new TypeReference<>() {});
            return new AiMessageResponse(rs.getLong("id"), rs.getString("role"), rs.getString("content"), sources,
                    rs.getObject("created_at", OffsetDateTime.class).toInstant());
        } catch (JsonProcessingException exception) {
            throw new SQLException("无法读取 AI 来源", exception);
        }
    }

    private AiConversationResponse withMessages(AiConversationResponse value, List<AiMessageResponse> messages) {
        return new AiConversationResponse(value.id(), value.courseId(), value.title(), messages,
                value.createdAt(), value.updatedAt());
    }
}
