CREATE TABLE benefit_program (
    id                  BIGSERIAL PRIMARY KEY,
    source              VARCHAR(20)  NOT NULL,          -- CENTRAL / LOCAL / RULE_TABLE
    serv_id             VARCHAR(50)  NOT NULL,
    name                VARCHAR(300) NOT NULL,
    agency              VARCHAR(200),
    region_code         VARCHAR(10),                    -- 지자체 제도만. 전국(중앙부처)은 NULL
    target_text         TEXT,
    content_text        TEXT,
    apply_text          TEXT,
    deadline_rule       VARCHAR(500),
    source_url          VARCHAR(500),
    embedding_generated BOOLEAN      NOT NULL DEFAULT FALSE,
    raw_last_mod_date   DATE,
    last_synced_at      TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uk_benefit_program_source_serv UNIQUE (source, serv_id)
);

CREATE INDEX idx_benefit_program_region ON benefit_program (region_code);

CREATE TABLE benefit_program_required_docs (
    program_id BIGINT       NOT NULL REFERENCES benefit_program (id) ON DELETE CASCADE,
    doc_name   VARCHAR(200) NOT NULL
);

-- W3 단계에서 pgvector 확장 + embedding 컬럼 추가 예정:
-- CREATE EXTENSION IF NOT EXISTS vector;
-- ALTER TABLE benefit_program ADD COLUMN embedding vector(1536);
