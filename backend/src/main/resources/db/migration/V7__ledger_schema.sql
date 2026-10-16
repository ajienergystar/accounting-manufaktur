-- =====================================================================
-- V7: Buku besar — jurnal, baris jurnal, jurnal berulang, lampiran
-- =====================================================================

CREATE TABLE journal_entries (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    journal_no      VARCHAR(30)    NOT NULL,
    journal_date    DATE           NOT NULL,
    period_id       BIGINT         NOT NULL,
    journal_type    VARCHAR(20)    NOT NULL,
    source_type     VARCHAR(40)    NOT NULL DEFAULT 'MANUAL',
    source_id       BIGINT,
    source_no       VARCHAR(30),
    description     VARCHAR(500)   NOT NULL,
    status          VARCHAR(10)    NOT NULL,
    total_debit     DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_credit    DECIMAL(19, 2) NOT NULL DEFAULT 0,
    posted_at       DATETIME(6),
    posted_by       VARCHAR(100),
    reversal_of_id  BIGINT,
    reversed_by_id  BIGINT,
    created_at      DATETIME(6)    NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_no (journal_no),
    KEY ix_journal_date_status (journal_date, status),
    KEY ix_journal_source (source_type, source_id),
    KEY ix_journal_period (period_id),
    CONSTRAINT fk_journal_period FOREIGN KEY (period_id) REFERENCES periods (id),
    CONSTRAINT fk_journal_reversal_of FOREIGN KEY (reversal_of_id) REFERENCES journal_entries (id),
    CONSTRAINT fk_journal_reversed_by FOREIGN KEY (reversed_by_id) REFERENCES journal_entries (id),
    CONSTRAINT ck_journal_type CHECK (journal_type IN ('GENERAL', 'ADJUSTMENT', 'AUTO', 'REVERSAL', 'OPENING', 'CLOSING')),
    CONSTRAINT ck_journal_status CHECK (status IN ('DRAFT', 'POSTED', 'VOID')),
    CONSTRAINT ck_journal_balanced CHECK (status <> 'POSTED' OR total_debit = total_credit)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE journal_lines (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    journal_entry_id    BIGINT         NOT NULL,
    line_no             SMALLINT       NOT NULL,
    account_id          BIGINT         NOT NULL,
    department_id       BIGINT,
    description         VARCHAR(255),
    debit               DECIMAL(19, 2) NOT NULL DEFAULT 0,
    credit              DECIMAL(19, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY ix_jl_entry (journal_entry_id),
    KEY ix_jl_account (account_id),
    KEY ix_jl_department (department_id),
    CONSTRAINT fk_jl_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_jl_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_jl_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_jl_non_negative CHECK (debit >= 0 AND credit >= 0),
    CONSTRAINT ck_jl_one_side CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Template jurnal berulang (mis. sewa bulanan, amortisasi)
CREATE TABLE recurring_journals (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150) NOT NULL,
    description     VARCHAR(500) NOT NULL,
    day_of_month    TINYINT      NOT NULL,
    next_run_date   DATE         NOT NULL,
    end_date        DATE,
    auto_post       BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_run_at     DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ck_recurring_day CHECK (day_of_month BETWEEN 1 AND 28)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE recurring_journal_lines (
    id                      BIGINT         NOT NULL AUTO_INCREMENT,
    recurring_journal_id    BIGINT         NOT NULL,
    line_no                 SMALLINT       NOT NULL,
    account_id              BIGINT         NOT NULL,
    department_id           BIGINT,
    description             VARCHAR(255),
    debit                   DECIMAL(19, 2) NOT NULL DEFAULT 0,
    credit                  DECIMAL(19, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_rjl_header FOREIGN KEY (recurring_journal_id) REFERENCES recurring_journals (id) ON DELETE CASCADE,
    CONSTRAINT fk_rjl_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_rjl_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_rjl_one_side CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Lampiran dokumen (bukti transaksi) untuk entitas apa pun
CREATE TABLE attachments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type     VARCHAR(40)  NOT NULL,
    entity_id       BIGINT       NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    storage_key     VARCHAR(255) NOT NULL,
    uploaded_by     VARCHAR(100) NOT NULL,
    uploaded_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attachments_key (storage_key),
    KEY ix_attachments_entity (entity_type, entity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
