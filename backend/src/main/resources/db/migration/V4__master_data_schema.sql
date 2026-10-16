-- =====================================================================
-- V4: Master data — periode, bagan akun, mapping akun, pajak, satuan,
--     gudang, departemen, pelanggan, pemasok, item
-- =====================================================================

CREATE TABLE fiscal_years (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    year            SMALLINT    NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at       DATETIME(6),
    closed_by       VARCHAR(100),
    created_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fiscal_years_year (year),
    CONSTRAINT ck_fy_status CHECK (status IN ('OPEN', 'CLOSED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE periods (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    fiscal_year_id  BIGINT      NOT NULL,
    period_no       TINYINT     NOT NULL,
    name            VARCHAR(30) NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at       DATETIME(6),
    closed_by       VARCHAR(100),
    created_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_periods_fy_no (fiscal_year_id, period_no),
    UNIQUE KEY uk_periods_start (start_date),
    KEY ix_periods_range (start_date, end_date),
    CONSTRAINT fk_periods_fy FOREIGN KEY (fiscal_year_id) REFERENCES fiscal_years (id),
    CONSTRAINT ck_period_status CHECK (status IN ('OPEN', 'CLOSED', 'LOCKED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE accounts (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    code                VARCHAR(20)  NOT NULL,
    name                VARCHAR(150) NOT NULL,
    parent_id           BIGINT,
    account_type        VARCHAR(30)  NOT NULL,
    normal_balance      VARCHAR(6)   NOT NULL,
    is_header           BOOLEAN      NOT NULL DEFAULT FALSE,
    is_cash_bank        BOOLEAN      NOT NULL DEFAULT FALSE,
    cash_flow_category  VARCHAR(20),
    level               TINYINT      NOT NULL DEFAULT 1,
    description         VARCHAR(255),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_at          DATETIME(6),
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_code (code),
    KEY ix_accounts_parent (parent_id),
    KEY ix_accounts_type (account_type),
    CONSTRAINT fk_accounts_parent FOREIGN KEY (parent_id) REFERENCES accounts (id),
    CONSTRAINT ck_accounts_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'COST_OF_GOODS',
        'EXPENSE', 'OTHER_INCOME', 'OTHER_EXPENSE', 'INCOME_TAX')),
    CONSTRAINT ck_accounts_nb CHECK (normal_balance IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_accounts_cf CHECK (cash_flow_category IS NULL OR cash_flow_category IN ('CASH', 'OPERATING', 'INVESTING', 'FINANCING'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Akun default untuk jurnal otomatis (tidak ada akun yang di-hardcode di kode)
CREATE TABLE account_mappings (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    mapping_key     VARCHAR(50)  NOT NULL,
    account_id      BIGINT       NOT NULL,
    group_name      VARCHAR(50)  NOT NULL,
    description     VARCHAR(255) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_mappings_key (mapping_key),
    CONSTRAINT fk_account_mappings_account FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE tax_rates (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)   NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    tax_type        VARCHAR(20)   NOT NULL,
    rate            DECIMAL(7, 4) NOT NULL,
    account_id      BIGINT,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)   NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tax_rates_code (code),
    CONSTRAINT fk_tax_rates_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT ck_tax_type CHECK (tax_type IN ('PPN', 'PPH21', 'PPH22', 'PPH23')),
    CONSTRAINT ck_tax_rate CHECK (rate >= 0 AND rate <= 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE uoms (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_uoms_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 1 from_uom = factor x to_uom (contoh: 1 BOX = 12 PCS)
CREATE TABLE uom_conversions (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    from_uom_id     BIGINT         NOT NULL,
    to_uom_id       BIGINT         NOT NULL,
    factor          DECIMAL(19, 6) NOT NULL,
    created_at      DATETIME(6)    NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_uom_conv (from_uom_id, to_uom_id),
    CONSTRAINT fk_uom_conv_from FOREIGN KEY (from_uom_id) REFERENCES uoms (id),
    CONSTRAINT fk_uom_conv_to FOREIGN KEY (to_uom_id) REFERENCES uoms (id),
    CONSTRAINT ck_uom_conv_factor CHECK (factor > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE warehouses (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(255),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouses_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE departments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    dept_type       VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_departments_code (code),
    CONSTRAINT ck_dept_type CHECK (dept_type IN ('PRODUCTION', 'SERVICE', 'NON_PRODUCTION'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE customers (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    code                VARCHAR(20)    NOT NULL,
    name                VARCHAR(150)   NOT NULL,
    npwp                VARCHAR(30),
    address             VARCHAR(500),
    city                VARCHAR(100),
    phone               VARCHAR(30),
    email               VARCHAR(150),
    contact_person      VARCHAR(100),
    payment_term_days   INT            NOT NULL DEFAULT 30,
    credit_limit        DECIMAL(19, 2) NOT NULL DEFAULT 0,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          DATETIME(6)    NOT NULL,
    created_by          VARCHAR(100),
    updated_at          DATETIME(6),
    updated_by          VARCHAR(100),
    version             BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customers_code (code),
    KEY ix_customers_name (name),
    CONSTRAINT ck_customers_term CHECK (payment_term_days >= 0),
    CONSTRAINT ck_customers_limit CHECK (credit_limit >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE suppliers (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    code                VARCHAR(20)  NOT NULL,
    name                VARCHAR(150) NOT NULL,
    npwp                VARCHAR(30),
    address             VARCHAR(500),
    city                VARCHAR(100),
    phone               VARCHAR(30),
    email               VARCHAR(150),
    contact_person      VARCHAR(100),
    payment_term_days   INT          NOT NULL DEFAULT 30,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_at          DATETIME(6),
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_suppliers_code (code),
    KEY ix_suppliers_name (name),
    CONSTRAINT ck_suppliers_term CHECK (payment_term_days >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE item_categories (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_categories_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE items (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    code            VARCHAR(30)    NOT NULL,
    name            VARCHAR(150)   NOT NULL,
    item_type       VARCHAR(30)    NOT NULL,
    category_id     BIGINT,
    uom_id          BIGINT         NOT NULL,
    standard_cost   DECIMAL(19, 4) NOT NULL DEFAULT 0,
    selling_price   DECIMAL(19, 2) NOT NULL DEFAULT 0,
    min_stock       DECIMAL(19, 4) NOT NULL DEFAULT 0,
    reorder_point   DECIMAL(19, 4) NOT NULL DEFAULT 0,
    is_stocked      BOOLEAN        NOT NULL DEFAULT TRUE,
    description     VARCHAR(500),
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)    NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_items_code (code),
    KEY ix_items_type (item_type),
    KEY ix_items_name (name),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES item_categories (id),
    CONSTRAINT fk_items_uom FOREIGN KEY (uom_id) REFERENCES uoms (id),
    CONSTRAINT ck_items_type CHECK (item_type IN ('RAW_MATERIAL', 'SUPPORTING_MATERIAL', 'WORK_IN_PROCESS',
        'FINISHED_GOOD', 'SUPPLIES', 'SERVICE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
