// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import java.util.List;

import javax.swing.ImageIcon;

import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.model.Channel;
import com.mirth.connect.plugins.ClientPlugin;

/**
 * Plugin that adds "View History" action to the Channel panel.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class ChannelHistoryPlugin extends ClientPlugin {

    private Frame parent;

    public ChannelHistoryPlugin(String name) {
        super(ChannelHistoryServletInterface.PLUGIN_NAME);
    }

    @Override
    public String getPluginPointName() {
        return "Channel History";
    }

    @Override
    public void start() {
        parent = PlatformUI.MIRTH_FRAME;

        // Add the "View History" task to the channel tasks
        parent.addTask("viewChannelHistory", "View History", "View version history of the selected channel.", "",
                new ImageIcon(Frame.class.getResource("images/book_previous.png")),
                parent.channelPanel.channelTasks, parent.channelPanel.channelPopupMenu, this);
    }

    @Override
    public void stop() {
    }

    @Override
    public void reset() {
    }

    public void viewChannelHistory() {
        List<Channel> selectedChannels = parent.channelPanel.getSelectedChannels();
        boolean isGroupSelected = parent.channelPanel.isGroupSelected();

        if (selectedChannels.size() != 1 || isGroupSelected) {
            parent.alertWarning(parent, "Please select a single channel to view its history.");
            return;
        }

        Channel selectedChannel = selectedChannels.get(0);
        ChannelHistoryDialog dialog = new ChannelHistoryDialog(parent, selectedChannel.getId(), selectedChannel.getName());
        dialog.setVisible(true);
    }
}
