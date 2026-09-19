CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    cover_url VARCHAR(500),
    creator_id BIGINT NOT NULL REFERENCES users(id),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_courses_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_courses_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_courses_creator_created ON courses(creator_id, created_at DESC);
CREATE INDEX idx_courses_visibility_status_created ON courses(visibility, status, created_at DESC);
CREATE INDEX idx_courses_title_lower ON courses(LOWER(title));

CREATE TABLE course_members (
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, user_id)
);

CREATE INDEX idx_course_members_user_joined ON course_members(user_id, joined_at DESC);

CREATE TABLE chapters (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_chapters_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX idx_chapters_course_order ON chapters(course_id, sort_order, id);
