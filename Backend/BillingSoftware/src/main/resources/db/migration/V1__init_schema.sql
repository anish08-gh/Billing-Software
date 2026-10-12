-- ============================================================
-- V1: Initial Database Schema
-- Fireworks Shop Billing SaaS
-- ============================================================

-- ============================================================
-- SHOPS / TENANTS
-- ============================================================

CREATE TABLE shops (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(15),
    email VARCHAR(255),
    address TEXT,
    logo_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'SHOP_OWNER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id),

    CONSTRAINT uq_users_email
        UNIQUE (email)
);

-- ============================================================
-- ITEMS / PRODUCT MASTER
-- ============================================================

CREATE TABLE items (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    sku VARCHAR(100),
    unit VARCHAR(30) NOT NULL DEFAULT 'PCS',
    price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_items_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id),

    CONSTRAINT chk_items_price
        CHECK (price >= 0),

    CONSTRAINT uq_items_shop_sku
        UNIQUE (shop_id, sku)
);

-- ============================================================
-- BILLS
-- ============================================================

CREATE TABLE bills (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL,
    bill_number VARCHAR(50) NOT NULL,

    customer_name VARCHAR(150),
    customer_phone VARCHAR(15),
    customer_email VARCHAR(255),
    customer_address TEXT,

    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,

    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    bill_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bills_shop
        FOREIGN KEY (shop_id)
        REFERENCES shops(id),

    CONSTRAINT uq_bills_shop_bill_number
        UNIQUE (shop_id, bill_number),

    CONSTRAINT chk_bills_amounts
        CHECK (
            subtotal >= 0
            AND discount_amount >= 0
            AND tax_amount >= 0
            AND total_amount >= 0
        )
);

-- ============================================================
-- BILL ITEMS
-- ============================================================

CREATE TABLE bill_items (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL,
    item_id UUID,

    item_name VARCHAR(200) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bill_items_bill
        FOREIGN KEY (bill_id)
        REFERENCES bills(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bill_items_item
        FOREIGN KEY (item_id)
        REFERENCES items(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_bill_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_bill_items_prices
        CHECK (
            unit_price >= 0
            AND discount_amount >= 0
            AND total_amount >= 0
        )
);

-- ============================================================
-- PAYMENTS
-- ============================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL,

    amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(100),
    notes TEXT,

    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payments_bill
        FOREIGN KEY (bill_id)
        REFERENCES bills(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payments_amount
        CHECK (amount > 0)
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_users_shop_id
    ON users(shop_id);

CREATE INDEX idx_items_shop_id
    ON items(shop_id);

CREATE INDEX idx_items_shop_name
    ON items(shop_id, name);

CREATE INDEX idx_bills_shop_id
    ON bills(shop_id);

CREATE INDEX idx_bills_shop_created_at
    ON bills(shop_id, created_at);

CREATE INDEX idx_bills_shop_payment_status
    ON bills(shop_id, payment_status);

CREATE INDEX idx_bill_items_bill_id
    ON bill_items(bill_id);

CREATE INDEX idx_payments_bill_id
    ON payments(bill_id);

CREATE INDEX idx_payments_bill_paid_at
    ON payments(bill_id, paid_at);