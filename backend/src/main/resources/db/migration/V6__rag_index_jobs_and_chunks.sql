CREATE TABLE rag_index_jobs (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    error VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_rag_index_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_rag_index_jobs_attempts CHECK (attempts >= 0)
);

CREATE UNIQUE INDEX uk_rag_index_jobs_active_resource
    ON rag_index_jobs(resource_id) WHERE status IN ('PENDING', 'RUNNING');
CREATE INDEX idx_rag_index_jobs_pending
    ON rag_index_jobs(status, created_at, id);

CREATE TABLE rag_chunks (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    chapter_id BIGINT REFERENCES chapters(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    file_id BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    page INTEGER,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding DOUBLE PRECISION[] NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_rag_chunks_resource_index UNIQUE(resource_id, chunk_index),
    CONSTRAINT ck_rag_chunks_page CHECK (page IS NULL OR page > 0),
    CONSTRAINT ck_rag_chunks_index CHECK (chunk_index >= 0),
    CONSTRAINT ck_rag_chunks_content CHECK (LENGTH(content) > 0)
);

CREATE INDEX idx_rag_chunks_course ON rag_chunks(course_id);
CREATE INDEX idx_rag_chunks_chapter ON rag_chunks(chapter_id) WHERE chapter_id IS NOT NULL;
CREATE INDEX idx_rag_chunks_resource ON rag_chunks(resource_id);

INSERT INTO schema_metadata(component) VALUES ('m6-rag-indexing');
