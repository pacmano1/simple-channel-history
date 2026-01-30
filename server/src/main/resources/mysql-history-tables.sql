CREATE TABLE IF NOT EXISTS channel_history (
    id              INTEGER PRIMARY KEY AUTO_INCREMENT,
    revision        INTEGER NOT NULL,
    channel_id      CHAR(36) NOT NULL,
    user_id         INTEGER NOT NULL,
    date_created    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    channel         LONGTEXT,
    INDEX idx_channel_history_channel_id (channel_id)
);

CREATE TABLE IF NOT EXISTS code_template_history (
    id                  INTEGER PRIMARY KEY AUTO_INCREMENT,
    revision            INTEGER NOT NULL,
    code_template_id    CHAR(36) NOT NULL,
    user_id             INTEGER NOT NULL,
    date_created        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    code_template       LONGTEXT,
    INDEX idx_code_template_history_ct_id (code_template_id)
);
