-- =====================================================================
-- V8: Persediaan — saldo stok, mutasi, layer FIFO, penyesuaian, transfer, opname
-- =====================================================================

CREATE TABLE stock_balances (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    item_id         BIGINT         NOT NULL,
    warehouse_id    BIGINT         NOT NULL,
    qty             DECIMAL(19, 4) NOT NULL DEFAULT 0,
    total_value     DECIMAL(19, 2) NOT NULL DEFAULT 0,
    avg_cost        DECIMAL(19, 4) NOT NULL DEFAULT 0,
    last_movement_date DATE,
    version         BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_balance (item_id, warehouse_id),
    KEY ix_stock_balance_wh (warehouse_id),
    CONSTRAINT fk_sb_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_sb_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE stock_movements (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    movement_date   DATE           NOT NULL,
    item_id         BIGINT         NOT NULL,
    warehouse_id    BIGINT         NOT NULL,
    direction       VARCHAR(3)     NOT NULL,
    qty             DECIMAL(19, 4) NOT NULL,
    unit_cost       DECIMAL(19, 4) NOT NULL,
    total_cost      DECIMAL(19, 2) NOT NULL,
    qty_after       DECIMAL(19, 4) NOT NULL,
    value_after     DECIMAL(19, 2) NOT NULL,
    source_type     VARCHAR(40)    NOT NULL,
    source_id       BIGINT,
    source_no       VARCHAR(30),
    description     VARCHAR(255),
    created_at      DATETIME(6)    NOT NULL,
    created_by      VARCHAR(100),
    PRIMARY KEY (id),
    KEY ix_sm_item_wh_date (item_id, warehouse_id, movement_date),
    KEY ix_sm_source (source_type, source_id),
    KEY ix_sm_date (movement_date),
    CONSTRAINT fk_sm_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_sm_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_sm_direction CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_sm_qty CHECK (qty > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Layer biaya untuk metode FIFO
CREATE TABLE stock_cost_layers (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    item_id         BIGINT         NOT NULL,
    warehouse_id    BIGINT         NOT NULL,
    movement_id     BIGINT         NOT NULL,
    layer_date      DATE           NOT NULL,
    qty_in          DECIMAL(19, 4) NOT NULL,
    qty_remaining   DECIMAL(19, 4) NOT NULL,
    unit_cost       DECIMAL(19, 4) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_layer_open (item_id, warehouse_id, qty_remaining, layer_date, id),
    CONSTRAINT fk_layer_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_layer_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_layer_mv FOREIGN KEY (movement_id) REFERENCES stock_movements (id),
    CONSTRAINT ck_layer_remaining CHECK (qty_remaining >= 0 AND qty_remaining <= qty_in)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Penyesuaian stok (termasuk saldo awal persediaan)
CREATE TABLE stock_adjustments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    doc_no          VARCHAR(30)  NOT NULL,
    doc_date        DATE         NOT NULL,
    warehouse_id    BIGINT       NOT NULL,
    adjustment_type VARCHAR(20)  NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    journal_id      BIGINT,
    total_value     DECIMAL(19, 2) NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_adj_no (doc_no),
    CONSTRAINT fk_adj_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_adj_journal FOREIGN KEY (journal_id) REFERENCES journal_entries (id),
    CONSTRAINT ck_adj_type CHECK (adjustment_type IN ('ADJUSTMENT', 'OPENING', 'OPNAME')),
    CONSTRAINT ck_adj_status CHECK (status IN ('POSTED', 'VOID'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE stock_adjustment_lines (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    adjustment_id   BIGINT         NOT NULL,
    line_no         SMALLINT       NOT NULL,
    item_id         BIGINT         NOT NULL,
    qty_change      DECIMAL(19, 4) NOT NULL,
    unit_cost       DECIMAL(19, 4) NOT NULL,
    total_value     DECIMAL(19, 2) NOT NULL,
    note            VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_adjl_header FOREIGN KEY (adjustment_id) REFERENCES stock_adjustments (id) ON DELETE CASCADE,
    CONSTRAINT fk_adjl_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT ck_adjl_qty CHECK (qty_change <> 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE stock_transfers (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    doc_no              VARCHAR(30)  NOT NULL,
    doc_date            DATE         NOT NULL,
    from_warehouse_id   BIGINT       NOT NULL,
    to_warehouse_id     BIGINT       NOT NULL,
    notes               VARCHAR(500),
    status              VARCHAR(10)  NOT NULL,
    total_value         DECIMAL(19, 2) NOT NULL DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_at          DATETIME(6),
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trf_no (doc_no),
    CONSTRAINT fk_trf_from FOREIGN KEY (from_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_trf_to FOREIGN KEY (to_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT ck_trf_diff CHECK (from_warehouse_id <> to_warehouse_id),
    CONSTRAINT ck_trf_status CHECK (status IN ('POSTED', 'VOID'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE stock_transfer_lines (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    transfer_id     BIGINT         NOT NULL,
    line_no         SMALLINT       NOT NULL,
    item_id         BIGINT         NOT NULL,
    qty             DECIMAL(19, 4) NOT NULL,
    unit_cost       DECIMAL(19, 4) NOT NULL DEFAULT 0,
    total_value     DECIMAL(19, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_trfl_header FOREIGN KEY (transfer_id) REFERENCES stock_transfers (id) ON DELETE CASCADE,
    CONSTRAINT fk_trfl_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT ck_trfl_qty CHECK (qty > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE stock_opnames (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    doc_no          VARCHAR(30)  NOT NULL,
    doc_date        DATE         NOT NULL,
    warehouse_id    BIGINT       NOT NULL,
    notes           VARCHAR(500),
    status          VARCHAR(10)  NOT NULL,
    adjustment_id   BIGINT,
    created_at      DATETIME(6)  NOT NULL,
    created_by      VARCHAR(100),
    updated_at      DATETIME(6),
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_opname_no (doc_no),
    CONSTRAINT fk_opname_wh FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_opname_adj FOREIGN KEY (adjustment_id) REFERENCES stock_adjustments (id),
    CONSTRAINT ck_opname_status CHECK (status IN ('DRAFT', 'POSTED', 'VOID'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE stock_opname_lines (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    opname_id       BIGINT         NOT NULL,
    item_id         BIGINT         NOT NULL,
    system_qty      DECIMAL(19, 4) NOT NULL,
    counted_qty     DECIMAL(19, 4),
    unit_cost       DECIMAL(19, 4) NOT NULL DEFAULT 0,
    note            VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_opname_item (opname_id, item_id),
    CONSTRAINT fk_opl_header FOREIGN KEY (opname_id) REFERENCES stock_opnames (id) ON DELETE CASCADE,
    CONSTRAINT fk_opl_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
