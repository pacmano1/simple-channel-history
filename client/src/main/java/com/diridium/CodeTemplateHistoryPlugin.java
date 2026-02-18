// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.codetemplate.CodeTemplatePanel;
import com.mirth.connect.plugins.ClientPlugin;

/**
 * Plugin that adds "View History" action to the Code Template panel.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class CodeTemplateHistoryPlugin extends ClientPlugin {

    public CodeTemplateHistoryPlugin(String name) {
        super(ChannelHistoryServletInterface.PLUGIN_NAME);
    }

    @Override
    public String getPluginPointName() {
        return "Code Template History";
    }

    @Override
    public void start() {
        // Add the "View History" action to the code template panel
        CodeTemplatePanel codeTemplatePanel = PlatformUI.MIRTH_FRAME.codeTemplatePanel;
        if (codeTemplatePanel != null) {
            AbstractAction viewHistoryAction = new AbstractAction("View History", new ImageIcon(Frame.class.getResource("images/book_previous.png"))) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showCodeTemplateHistory();
                }
            };
            viewHistoryAction.putValue(AbstractAction.SHORT_DESCRIPTION, "View version history of the selected code template");

            // Add the action to the code template panel's task pane
            codeTemplatePanel.addAction(viewHistoryAction, new HashSet<>(Collections.singletonList("onlySingleCodeTemplates")), "doViewCodeTemplateHistory");
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public void reset() {
    }

    private void showCodeTemplateHistory() {
        CodeTemplatePanel codeTemplatePanel = PlatformUI.MIRTH_FRAME.codeTemplatePanel;
        String selectedId = codeTemplatePanel.getCurrentSelectedId();

        if (selectedId == null) {
            PlatformUI.MIRTH_FRAME.alertWarning(PlatformUI.MIRTH_FRAME, "Please select a code template to view its history.");
            return;
        }

        // Get the code template name for display
        String codeTemplateName = "Code Template";
        var cachedTemplates = codeTemplatePanel.getCachedCodeTemplates();
        if (cachedTemplates != null && cachedTemplates.containsKey(selectedId)) {
            codeTemplateName = cachedTemplates.get(selectedId).getName();
        }

        CodeTemplateHistoryDialog dialog = new CodeTemplateHistoryDialog(PlatformUI.MIRTH_FRAME, selectedId, codeTemplateName);
        dialog.setVisible(true);
    }
}
