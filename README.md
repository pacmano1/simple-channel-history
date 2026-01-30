# Simple Channel History

An Open Integration Engine plugin for tracking version history of Channels and Code Templates.

## Features

- Automatic version history tracking when channels and code templates are saved
- Side-by-side diff viewer to compare any two versions
- Revert to any previous version
- Prune older versions to manage storage
- Database-backed storage (history travels with database backups)

## Supported Databases

- PostgreSQL
- MySQL
- Oracle
- SQL Server
- Derby

## Building

Requires OIE libraries in your Maven repository (local or remote).

```bash
mvn clean package
```

The plugin zip will be in `package/target/simple-channel-history-<version>.zip`.

## Installation

1. Stop OIE
2. Extract the zip to the `extensions` directory
3. Start OIE
4. The plugin will create the necessary database tables on first startup

## Usage

### Viewing History
- Select a channel and click "View History" in the Channel Tasks panel
- Or right-click a channel and select "View History"

### Comparing Versions
- Select two versions in the history table
- Right-click and select "Show Diff"

### Reverting
- Select a version in the history table
- Right-click and select "Revert to this version"

### Pruning
- Select a version in the history table
- Right-click and select "Prune older revisions"
- All versions older than the selected version will be deleted

## License

Apache License 2.0

Based on the original git-ext plugin by Kiran Ayyagari.
