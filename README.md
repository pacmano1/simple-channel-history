# Simple Channel and Code Template History

An Open Integration Engine plugin for tracking version history of Channels and Code Templates.

## Features

- Automatic version history tracking when channels and code templates are saved
- Decomposed component diff view with navigable tree (scripts, connectors, filter/transformer steps, plugin properties)
- Side-by-side diff viewer with word-level inline highlighting
- Color-coded change indicators (added, removed, modified, unchanged)
- Revert to any previous version
- Prune older versions to manage storage
- Database-backed storage (history travels with database backups)

<img src="https://raw.githubusercontent.com/wiki/pacmano1/simple-channel-history/images/4.jpg" alt="History dialog showing revision table" width="500">

<img src="https://raw.githubusercontent.com/wiki/pacmano1/simple-channel-history/images/5.jpg" alt="Decomposed component diff view with tree navigation and side-by-side comparison" width="700">

## Supported Databases

- PostgreSQL
- MySQL
- Oracle
- SQL Server
- Derby

## Building

Requires Java 17+ and OIE libraries in your Maven repository (local or remote).

### Development Build (unsigned)

```bash
mvn clean package
```

### Release Build (signed with YubiKey)

Signed builds require a YubiKey with a code signing certificate and the OpenSC PKCS#11 library.

1. Copy `yubikey-pkcs11.cfg.example` to `yubikey-pkcs11.cfg` and update the library path for your system
2. Create `certchain.pem` containing your certificate chain (your cert + intermediate CA + root CA in PEM format)
3. Build with the signing profile:

```bash
mvn clean package -Psigning -Dsigning.storepass=<your-yubikey-pin>
```

Or set the PIN via environment variable:

```bash
export YUBIKEY_PIN=<your-pin>
mvn clean package -Psigning
```

The plugin zip will be in `package/target/simple-channel-history-<version>.zip`.

## Installation

Install using the Extensions manager in the OIE Administrator, or manually extract
to the `extensions` directory. A restart is required after installation.

The plugin will create the necessary database tables on first startup.

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
