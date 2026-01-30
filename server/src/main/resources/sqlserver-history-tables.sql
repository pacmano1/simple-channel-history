IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'channel_history') AND type in (N'U'))
CREATE TABLE channel_history (
    id              INTEGER IDENTITY(1,1) PRIMARY KEY,
    revision        INTEGER NOT NULL,
    channel_id      CHAR(36) NOT NULL,
    user_id         INTEGER NOT NULL,
    date_created    DATETIME2 DEFAULT GETDATE(),
    channel         NVARCHAR(MAX)
)

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_channel_history_channel_id')
CREATE INDEX idx_channel_history_channel_id ON channel_history(channel_id)

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'code_template_history') AND type in (N'U'))
CREATE TABLE code_template_history (
    id                  INTEGER IDENTITY(1,1) PRIMARY KEY,
    revision            INTEGER NOT NULL,
    code_template_id    CHAR(36) NOT NULL,
    user_id             INTEGER NOT NULL,
    date_created        DATETIME2 DEFAULT GETDATE(),
    code_template       NVARCHAR(MAX)
)

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_code_template_history_ct_id')
CREATE INDEX idx_code_template_history_ct_id ON code_template_history(code_template_id)
