
-- ============================================================
-- V4: Bill and Audit History Foundation
-- Fireworks Shop Billing SaaS
-- ============================================================

-- 1. Maintain a sequence for bill numbers per shop.
ALTER TABLE shops
    ADD COLUMN bill_sequence BIGINT NOT NULL DEFAULT 0;

ALTER TABLE shops
    ADD CONSTRAINT chk_shops_bill_sequence
    CHECK (bill_sequence >= 0);


-- 2. Add optimistic locking to prevent silent overwrites
-- when two users edit the same bill concurrently.
ALTER TABLE bills
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE bills
    ADD CONSTRAINT chk_bills_version
    CHECK (version >= 0);


-- 3. Give each bill line a stable position.
ALTER TABLE bill_items
    ADD COLUMN line_number INTEGER;

-- Backfill line numbers safely for any existing records.
WITH numbered_items AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY bill_id
            ORDER BY created_at, id
        ) AS row_num
    FROM bill_items
)
UPDATE bill_items AS bi
SET line_number = numbered_items.row_num
FROM numbered_items
WHERE bi.id = numbered_items.id;

ALTER TABLE bill_items
    ALTER COLUMN line_number SET NOT NULL;

ALTER TABLE bill_items
    ADD CONSTRAINT chk_bill_items_line_number
    CHECK (line_number > 0);

ALTER TABLE bill_items
    ADD CONSTRAINT uq_bill_items_bill_line_number
    UNIQUE (bill_id, line_number);


-- 4. Store a complete snapshot of each bill creation,
-- update, or cancellation for permanent audit history.
CREATE TABLE bill_versions (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL,
    version_number BIGINT NOT NULL,
    changed_by_user_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    snapshot JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bill_versions_bill
        FOREIGN KEY (bill_id)
        REFERENCES bills(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bill_versions_user
        FOREIGN KEY (changed_by_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_bill_versions_bill_version
        UNIQUE (bill_id, version_number),

    CONSTRAINT chk_bill_versions_version
        CHECK (version_number > 0),

    CONSTRAINT chk_bill_versions_action
        CHECK (action IN ('CREATE', 'UPDATE', 'CANCEL'))
);


-- 5. Indexes for retrieving a bill's audit history efficiently.
CREATE INDEX idx_bill_versions_bill_created_at
    ON bill_versions (bill_id, created_at);

CREATE INDEX idx_bill_versions_changed_by
    ON bill_versions (changed_by_user_id, created_at);