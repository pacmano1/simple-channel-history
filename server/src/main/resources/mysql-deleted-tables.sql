CREATE TABLE IF NOT EXISTS deleted_channel (
    id              INTEGER PRIMARY KEY AUTO_INCREMENT,
    channel_id      CHAR(36) NOT NULL,
    name            VARCHAR(255),
    user_id         INTEGER NOT NULL,
    date_deleted    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content         LONGTEXT
);

CREATE TABLE IF NOT EXISTS deleted_code_template (
    id                  INTEGER PRIMARY KEY AUTO_INCREMENT,
    code_template_id    CHAR(36) NOT NULL,
    name                VARCHAR(255),
    user_id             INTEGER NOT NULL,
    date_deleted        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content             LONGTEXT
);
