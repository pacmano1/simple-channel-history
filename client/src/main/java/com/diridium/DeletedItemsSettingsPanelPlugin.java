// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.plugins.SettingsPanelPlugin;

/**
 * Settings panel plugin that adds a "Channel History: Deleted Items" tab
 * to the Administrator Settings area.
 */
public class DeletedItemsSettingsPanelPlugin extends SettingsPanelPlugin {

    public DeletedItemsSettingsPanelPlugin(String name) {
        super(ChannelHistoryServletInterface.PLUGIN_NAME);
    }

    @Override
    public String getPluginPointName() {
        return "Channel History: Deleted Items";
    }

    @Override
    public AbstractSettingsPanel getSettingsPanel() {
        return new DeletedItemsSettingsPanel("Channel History: Deleted Items");
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void reset() {
    }
}
