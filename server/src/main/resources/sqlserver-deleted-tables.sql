IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'deleted_channel') AND type in (N'U'))
CREATE TABLE deleted_channel (
    id              INTEGER IDENTITY(1,1) PRIMARY KEY,
    channel_id      CHAR(36) NOT NULL,
    name            NVARCHAR(255),
    user_id         INTEGER NOT NULL,
    date_deleted    DATETIME2 DEFAULT GETDATE(),
    content         NVARCHAR(MAX)
)

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'deleted_code_template') AND type in (N'U'))
CREATE TABLE deleted_code_template (
    id                  INTEGER IDENTITY(1,1) PRIMARY KEY,
    code_template_id    CHAR(36) NOT NULL,
    name                NVARCHAR(255),
    user_id             INTEGER NOT NULL,
    date_deleted        DATETIME2 DEFAULT GETDATE(),
    content             NVARCHAR(MAX)
)
