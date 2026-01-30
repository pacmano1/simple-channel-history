package com.diridium;

import com.mirth.connect.server.migration.Migrator;
import com.mirth.connect.model.util.MigrationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrator for creating history tables used by the simple-channel-history plugin.
 * Creates channel_history and code_template_history tables on first startup.
 */
public class HistoryMigrator extends Migrator {

    private static Logger log = LoggerFactory.getLogger(HistoryMigrator.class);

    @Override
    public void migrate() throws MigrationException {
        // Use absolute path (starts with /) to load from classpath root
        String scriptName = "/" + getDatabaseType() + "-history-tables.sql";
        log.info("Running history table migration script: {}", scriptName);
        try {
            executeScript(scriptName);
            log.info("History tables created successfully");
        } catch (Exception e) {
            // Tables may already exist from previous run
            log.info("History table migration: {}", e.getMessage());
        }
    }

    @Override
    public void migrateSerializedData() throws MigrationException {
        // No serialized data migration needed
    }
}
