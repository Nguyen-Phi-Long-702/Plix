-- ai_model_params 
CREATE TABLE ai_model_params (
    user_id TEXT PRIMARY KEY, -- sentinel '__seed__' cho model mac dinh
    vocabulary JSONB NOT NULL DEFAULT '{}',
    idf JSONB NOT NULL DEFAULT '{}',
    class_priors JSONB NOT NULL DEFAULT '{}',
    likelihoods JSONB NOT NULL DEFAULT '{}',
    trained_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    training_sample_count INTEGER NOT NULL DEFAULT 0, -- dem so correction, khong phai transaction
    is_training BOOLEAN NOT NULL DEFAULT false
);

-- anomaly_stats 
CREATE TABLE anomaly_stats (
    user_id TEXT NOT NULL,
    category_id TEXT NOT NULL,
    mean REAL,
    std REAL,
    sample_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, category_id)
);