CREATE TABLE channel_history (
    id              INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    revision        INTEGER NOT NULL,
    channel_id      CHAR(36) NOT NULL,
    user_id         INTEGER NOT NULL,
    date_created    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    channel         CLOB
)

CREATE INDEX idx_channel_history_channel_id ON channel_history(channel_id)

CREATE TABLE code_template_history (
    id                  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    revision            INTEGER NOT NULL,
    code_template_id    CHAR(36) NOT NULL,
    user_id             INTEGER NOT NULL,
    date_created        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    code_template       CLOB
)

CREATE INDEX idx_code_template_history_ct_id ON code_template_history(code_template_id)
