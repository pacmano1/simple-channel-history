CREATE TABLE IF NOT EXISTS deleted_channel (
    id              SERIAL PRIMARY KEY,
    channel_id      CHAR(36) NOT NULL,
    name            VARCHAR(255),
    user_id         INTEGER NOT NULL,
    date_deleted    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content         TEXT
);

CREATE TABLE IF NOT EXISTS deleted_code_template (
    id                  SERIAL PRIMARY KEY,
    code_template_id    CHAR(36) NOT NULL,
    name                VARCHAR(255),
    user_id             INTEGER NOT NULL,
    date_deleted        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content             TEXT
);
