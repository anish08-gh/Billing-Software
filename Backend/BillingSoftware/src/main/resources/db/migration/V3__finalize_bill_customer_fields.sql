-- ============================================================
-- V3: Finalize Bill Customer Fields
-- Fireworks Shop Billing SaaS
-- ============================================================

ALTER TABLE bills
    ALTER COLUMN customer_name SET NOT NULL,
    ALTER COLUMN customer_phone SET NOT NULL;

ALTER TABLE bills
    DROP COLUMN customer_email,
    DROP COLUMN customer_address;