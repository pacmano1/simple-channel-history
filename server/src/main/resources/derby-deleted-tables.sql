CREATE TABLE deleted_channel (
    id              INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    channel_id      CHAR(36) NOT NULL,
    name            VARCHAR(255),
    user_id         INTEGER NOT NULL,
    date_deleted    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content         CLOB
)

CREATE TABLE deleted_code_template (
    id                  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code_template_id    CHAR(36) NOT NULL,
    name                VARCHAR(255),
    user_id             INTEGER NOT NULL,
    date_deleted        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content             CLOB
)
