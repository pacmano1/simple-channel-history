package com.diridium;

/*
   Copyright [2024] [Kiran Ayyagari]
   Copyright [2025-2026] [Diridium Technologies Inc.]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

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
