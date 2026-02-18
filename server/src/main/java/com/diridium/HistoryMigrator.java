// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.mirth.connect.server.migration.Migrator;
import com.mirth.connect.model.util.MigrationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrator for creating history tables used by the simple-channel-history plugin.
 * Creates channel_history, code_template_history, deleted_channel, and
 * deleted_code_template tables on first startup.
 */
public class HistoryMigrator extends Migrator {

    private static final Logger log = LoggerFactory.getLogger(HistoryMigrator.class);

    @Override
    public void migrate() throws MigrationException {
        executeScriptSafely("/" + getDatabaseType() + "-history-tables.sql", "History tables");
        executeScriptSafely("/" + getDatabaseType() + "-deleted-tables.sql", "Deleted item tables");
    }

    private void executeScriptSafely(String scriptName, String description) {
        try {
            executeScript(scriptName);
            log.info("{} created successfully", description);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase(Locale.ROOT) : "";
            if (msg.contains("already exist")) {
                log.info("{} already exist, skipping", description);
            } else {
                log.warn("{} migration may have failed: {}", description, e.getMessage(), e);
            }
        }
    }

    @Override
    public List<String> getUninstallStatements() throws MigrationException {
        return Arrays.asList(
                "DROP TABLE channel_history",
                "DROP TABLE code_template_history",
                "DROP TABLE deleted_channel",
                "DROP TABLE deleted_code_template");
    }

    @Override
    public void migrateSerializedData() throws MigrationException {
        // No serialized data migration needed
    }
}
