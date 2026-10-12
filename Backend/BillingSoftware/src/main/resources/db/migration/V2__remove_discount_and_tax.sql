-- ============================================================
-- V2: Remove Discount and Tax
-- Fireworks Shop Billing SaaS
-- ============================================================

ALTER TABLE bills
    DROP COLUMN discount_amount,
    DROP COLUMN tax_amount;

ALTER TABLE bill_items
    DROP COLUMN discount_amount;