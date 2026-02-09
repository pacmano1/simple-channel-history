// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

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
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase(java.util.Locale.ROOT) : "";
            if (msg.contains("already exist")) {
                log.info("History tables already exist, skipping creation");
            } else {
                log.warn("History table migration may have failed: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void migrateSerializedData() throws MigrationException {
        // No serialized data migration needed
    }
}
