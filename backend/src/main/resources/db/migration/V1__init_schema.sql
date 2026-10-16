-- =====================================================================
-- V1: Skema dasar (perusahaan, penomoran dokumen, audit log)
-- Semua tabel: InnoDB, utf8mb4, kolom audit + version (optimistic lock)
-- =====================================================================

CREATE TABLE companies (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    code                        VARCHAR(20)  NOT NULL,
    name                        VARCHAR(150) NOT NULL,
    npwp                        VARCHAR(30),
    address                     VARCHAR(500),
    city                        VARCHAR(100),
    phone                       VARCHAR(30),
    email                       VARCHAR(150),
    logo_url                    VARCHAR(500),
    base_currency               CHAR(3)      NOT NULL DEFAULT 'IDR',
    fiscal_year_start_month     TINYINT      NOT NULL DEFAULT 1,
    inventory_costing_method    VARCHAR(20)  NOT NULL DEFAULT 'MOVING_AVERAGE',
    allow_negative_stock        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  DATETIME(6)  NOT NULL,
    created_by                  VARCHAR(100),
    updated_at                  DATETIME(6),
    updated_by                  VARCHAR(100),
    version                     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_companies_code (code),
    CONSTRAINT ck_companies_costing CHECK (inventory_costing_method IN ('MOVING_AVERAGE', 'FIFO')),
    CONSTRAINT ck_companies_fy_month CHECK (fiscal_year_start_month BETWEEN 1 AND 12)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Penomoran dokumen otomatis: prefix/tahun/bulan/urut, contoh JU/2026/10/0001
CREATE TABLE document_sequences (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    doc_type        VARCHAR(30) NOT NULL,
    prefix          VARCHAR(10) NOT NULL,
    period_year     SMALLINT    NOT NULL,
    period_month    TINYINT     NOT NULL,
    last_number     INT         NOT NULL DEFAULT 0,
    padding         TINYINT     NOT NULL DEFAULT 4,
    created_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_docseq (doc_type, period_year, period_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Audit trail: siapa, kapan, aksi, data sebelum/sesudah
CREATE TABLE audit_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    occurred_at     DATETIME(6)  NOT NULL,
    username        VARCHAR(100) NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    entity_type     VARCHAR(60)  NOT NULL,
    entity_id       VARCHAR(60),
    description     VARCHAR(500),
    before_data     JSON,
    after_data      JSON,
    ip_address      VARCHAR(64),
    PRIMARY KEY (id),
    KEY ix_audit_entity (entity_type, entity_id),
    KEY ix_audit_user_time (username, occurred_at),
    KEY ix_audit_time (occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
