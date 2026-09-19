CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    object_key VARCHAR(200) NOT NULL UNIQUE,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    uploader_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_files_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_files_status CHECK (status IN ('AVAILABLE', 'QUARANTINED'))
);

CREATE INDEX idx_files_uploader_created ON files(uploader_id, created_at DESC);

CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    chapter_id BIGINT REFERENCES chapters(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    resource_type VARCHAR(20) NOT NULL,
    file_id BIGINT REFERENCES files(id) ON DELETE SET NULL,
    external_url VARCHAR(2000),
    creator_id BIGINT NOT NULL REFERENCES users(id),
    view_count BIGINT NOT NULL DEFAULT 0,
    download_count BIGINT NOT NULL DEFAULT 0,
    ai_index_status VARCHAR(20) NOT NULL DEFAULT 'NOT_INDEXED',
    ai_index_error VARCHAR(1000),
    indexed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_resources_type CHECK (resource_type IN ('PDF','DOC','PPT','MARKDOWN','TXT','VIDEO','LINK','CODE','OTHER')),
    CONSTRAINT ck_resources_ai_status CHECK (ai_index_status IN ('NOT_INDEXED','PROCESSING','INDEXED','FAILED')),
    CONSTRAINT ck_resources_source CHECK (file_id IS NOT NULL OR external_url IS NOT NULL)
);

CREATE INDEX idx_resources_course_created ON resources(course_id, created_at DESC);
CREATE INDEX idx_resources_chapter_created ON resources(chapter_id, created_at DESC);
CREATE INDEX idx_resources_file ON resources(file_id) WHERE file_id IS NOT NULL;
CREATE INDEX idx_resources_title_lower ON resources(LOWER(title));

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_tags_name_lower ON tags(LOWER(name));

CREATE TABLE resource_tags (
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY(resource_id, tag_id)
);
