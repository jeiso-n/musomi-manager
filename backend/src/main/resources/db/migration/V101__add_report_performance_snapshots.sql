-- V101__add_report_performance_snapshots.sql
-- Store the exact class/stream and academic results used when each report PDF was generated.

ALTER TABLE generated_reports
    ADD COLUMN class_id BIGINT,
    ADD COLUMN stream_id BIGINT,
    ADD COLUMN class_name VARCHAR(50),
    ADD COLUMN stream_name VARCHAR(50),
    ADD COLUMN report_status VARCHAR(20),
    ADD COLUMN expected_assessment_count INTEGER,
    ADD COLUMN recorded_assessment_count INTEGER,
    ADD COLUMN overall_average NUMERIC(6, 2),
    ADD COLUMN overall_grade VARCHAR(2),
    ADD COLUMN class_position INTEGER,
    ADD COLUMN ranking_cohort_size INTEGER;

ALTER TABLE generated_reports
    ADD CONSTRAINT fk_generated_reports_class_id
        FOREIGN KEY (class_id) REFERENCES classes(id),
    ADD CONSTRAINT fk_generated_reports_stream_id
        FOREIGN KEY (stream_id) REFERENCES streams(id);

CREATE INDEX idx_generated_reports_school_class_term
    ON generated_reports(school_id, class_id, term_id);

CREATE TABLE generated_report_subjects (
    id BIGSERIAL PRIMARY KEY,
    generated_report_id BIGINT NOT NULL,
    subject_name VARCHAR(200) NOT NULL,
    average NUMERIC(6, 2),
    grade VARCHAR(2),
    expected_assessment_count INTEGER NOT NULL,
    recorded_assessment_count INTEGER NOT NULL,
    CONSTRAINT fk_generated_report_subjects_report
        FOREIGN KEY (generated_report_id) REFERENCES generated_reports(id) ON DELETE CASCADE
);

CREATE INDEX idx_generated_report_subjects_report
    ON generated_report_subjects(generated_report_id);
