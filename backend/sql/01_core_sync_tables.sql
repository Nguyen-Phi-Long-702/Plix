-- transactions
CREATE TABLE transactions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    amount BIGINT NOT NULL,
    type TEXT NOT NULL DEFAULT 'expense',
    category_id TEXT,
    note TEXT DEFAULT '',
    payment_method TEXT,
    occurred_at BIGINT NOT NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT false,
    recurrence_rule TEXT,
    recurrence_parent_id TEXT,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_tx_user_occurred ON transactions(user_id, occurred_at);
CREATE INDEX idx_tx_user_updated ON transactions(user_id, updated_at);

-- categories 
CREATE TABLE categories (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    name TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'expense',
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(user_id, name, type)
);
CREATE INDEX idx_categories_user ON categories(user_id);

-- budgets
CREATE TABLE budgets (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    period TEXT NOT NULL,
    category_id TEXT,
    limit_amount BIGINT NOT NULL,
    threshold_percent INTEGER NOT NULL DEFAULT 80,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(user_id, period, category_id)
);

-- goals
CREATE TABLE goals (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    target_amount BIGINT NOT NULL,
    current_amount BIGINT NOT NULL DEFAULT 0,
    deadline BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- corrections 
CREATE TABLE corrections (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    transaction_id TEXT NOT NULL,
    predicted_category_id TEXT,
    corrected_category_id TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_corrections_user_updated ON corrections(user_id, updated_at);
CREATE INDEX idx_corrections_user_tx ON corrections(user_id, transaction_id);