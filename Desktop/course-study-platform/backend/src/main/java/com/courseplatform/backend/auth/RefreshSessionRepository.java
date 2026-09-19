package com.courseplatform.backend.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshSessionRepository {
    private final JdbcClient jdbcClient;

    public RefreshSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void create(UUID id, long userId, String tokenHash, Instant expiresAt) {
        jdbcClient.sql("""
                INSERT INTO refresh_sessions (id, user_id, token_hash, expires_at)
                VALUES (:id, :userId, :tokenHash, :expiresAt)
                """)
                .param("id", id).param("userId", userId).param("tokenHash", tokenHash)
                .param("expiresAt", utc(expiresAt)).update();
    }

    public Optional<RefreshSession> findByHashForUpdate(String tokenHash) {
        return jdbcClient.sql("SELECT * FROM refresh_sessions WHERE token_hash = :tokenHash FOR UPDATE")
                .param("tokenHash", tokenHash).query(this::map).optional();
    }

    public void rotate(UUID oldId, UUID newId, Instant now) {
        jdbcClient.sql("""
                UPDATE refresh_sessions
                SET revoked_at = :now, last_used_at = :now, replaced_by = :newId
                WHERE id = :oldId AND revoked_at IS NULL
                """)
                .param("now", utc(now)).param("newId", newId).param("oldId", oldId).update();
    }

    public void revoke(String tokenHash, Instant now) {
        jdbcClient.sql("""
                UPDATE refresh_sessions SET revoked_at = COALESCE(revoked_at, :now), last_used_at = :now
                WHERE token_hash = :tokenHash
                """)
                .param("now", utc(now)).param("tokenHash", tokenHash).update();
    }

    private RefreshSession map(ResultSet rs, int rowNum) throws SQLException {
        return new RefreshSession(
                rs.getObject("id", UUID.class), rs.getLong("user_id"), rs.getString("token_hash"),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(), nullableInstant(rs, "revoked_at")
        );
    }

    private Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private OffsetDateTime utc(Instant value) {
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
