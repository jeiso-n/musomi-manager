ALTER TABLE topics
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_topics_school_active ON topics(school_id) WHERE is_active = TRUE;
