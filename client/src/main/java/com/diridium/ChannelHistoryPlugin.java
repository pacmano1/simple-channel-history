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
                new ImageIcon(com.mirth.connect.client.ui.Frame.class.getResource("images/book_previous.png")),
                parent.channelPanel.channelTasks, parent.channelPanel.channelPopupMenu, this);
    }

    @Override
    public void stop() {
    }

    @Override
    public void reset() {
    }

    public void viewChannelHistory() {
        Channel selectedChannel = parent.channelPanel.getSelectedChannels().get(0);
        if (selectedChannel == null) {
            parent.alertWarning(parent, "Please select a channel to view its history.");
            return;
        }

        ChannelHistoryDialog dialog = new ChannelHistoryDialog(parent, selectedChannel.getId(), selectedChannel.getName());
        dialog.setVisible(true);
    }
}
