CREATE TABLE schema_metadata (
    id BIGSERIAL PRIMARY KEY,
    component VARCHAR(100) NOT NULL UNIQUE,
    installed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_metadata (component) VALUES ('m0-foundation');
