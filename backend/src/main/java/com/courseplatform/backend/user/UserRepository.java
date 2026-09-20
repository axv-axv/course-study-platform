package com.courseplatform.backend.user;

import com.courseplatform.backend.common.api.PageResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<User> findById(long id) {
        return jdbcClient.sql("SELECT * FROM users WHERE id = :id")
                .param("id", id).query(this::map).optional();
    }

    public Optional<User> findByUsername(String username) {
        return jdbcClient.sql("SELECT * FROM users WHERE LOWER(username) = LOWER(:username)")
                .param("username", username).query(this::map).optional();
    }

    public boolean existsByEmail(String email, Long excludedUserId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String sql = excludedUserId == null
                ? "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)"
                : "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email) AND id <> :excludedId";
        JdbcClient.StatementSpec spec = jdbcClient.sql(sql).param("email", email);
        if (excludedUserId != null) {
            spec = spec.param("excludedId", excludedUserId);
        }
        return spec.query(Integer.class).single() > 0;
    }

    public User create(String username, String passwordHash, String email, UserRole role) throws DuplicateKeyException {
        long id = jdbcClient.sql("""
                INSERT INTO users (username, password_hash, email, nickname, role)
                VALUES (:username, :passwordHash, :email, :username, :role)
                RETURNING id
                """)
                .param("username", username)
                .param("passwordHash", passwordHash)
                .param("email", email)
                .param("role", role.name())
                .query(Long.class).single();
        return findById(id).orElseThrow();
    }

    public User updateProfile(long id, long version, String nickname, String email, String bio) {
        int updated = jdbcClient.sql("""
                UPDATE users
                SET nickname = :nickname, email = :email, bio = :bio,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND version = :version
                """)
                .param("nickname", nickname)
                .param("email", email)
                .param("bio", bio)
                .param("id", id)
                .param("version", version)
                .update();
        if (updated != 1) {
            throw new IllegalStateException("用户资料已被其他请求修改");
        }
        return findById(id).orElseThrow();
    }

    public User updateAvatar(long id, long version, String avatarUrl) {
        int updated = jdbcClient.sql("""
                UPDATE users SET avatar_url = :avatarUrl, version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = :id AND version = :version
                """).param("avatarUrl", avatarUrl).param("id", id).param("version", version).update();
        if (updated != 1) {
            throw new IllegalStateException("用户头像已被其他请求修改");
        }
        return findById(id).orElseThrow();
    }

    public void updateRole(long id, UserRole role) {
        jdbcClient.sql("UPDATE users SET role = :role, version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
                .param("role", role.name()).param("id", id).update();
    }

    public void updateStatus(long id, UserStatus status) {
        jdbcClient.sql("UPDATE users SET status = :status, version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
                .param("status", status.name()).param("id", id).update();
    }

    public void revokeRefreshSessions(long userId) {
        jdbcClient.sql("UPDATE refresh_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND revoked_at IS NULL")
                .param("userId", userId).update();
    }

    public PageResult<UserResponse> findPage(int page, int size, String keyword, UserRole role) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (keyword != null) {
            where.append(" AND (LOWER(username) LIKE :keyword OR LOWER(COALESCE(nickname, '')) LIKE :keyword OR LOWER(COALESCE(email, '')) LIKE :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        if (role != null) {
            where.append(" AND role = :role");
            params.put("role", role.name());
        }
        List<UserResponse> items = jdbcClient.sql("SELECT * FROM users" + where
                        + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset")
                .params(params).param("limit", size).param("offset", (page - 1) * size)
                .query((rs, rowNum) -> UserResponse.from(map(rs, rowNum))).list();
        long total = jdbcClient.sql("SELECT COUNT(*) FROM users" + where)
                .params(params).query(Long.class).single();
        return new PageResult<>(items, page, size, total);
    }

    private User map(ResultSet rs, int rowNum) throws SQLException {
        return new User(
                rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                rs.getString("email"), rs.getString("nickname"), rs.getString("bio"),
                rs.getString("avatar_url"), UserRole.valueOf(rs.getString("role")),
                UserStatus.valueOf(rs.getString("status")), rs.getLong("version"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
