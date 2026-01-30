CREATE TABLE IF NOT EXISTS channel_history (
    id              SERIAL PRIMARY KEY,
    revision        INTEGER NOT NULL,
    channel_id      CHAR(36) NOT NULL,
    user_id         INTEGER NOT NULL,
    date_created    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    channel         TEXT
);

CREATE INDEX IF NOT EXISTS idx_channel_history_channel_id ON channel_history(channel_id);

CREATE TABLE IF NOT EXISTS code_template_history (
    id                  SERIAL PRIMARY KEY,
    revision            INTEGER NOT NULL,
    code_template_id    CHAR(36) NOT NULL,
    user_id             INTEGER NOT NULL,
    date_created        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    code_template       TEXT
);

CREATE INDEX IF NOT EXISTS idx_code_template_history_ct_id ON code_template_history(code_template_id);
