// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * The main window for showing diff.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class DiffWindow extends JDialog {
    private String leftLabel;
    private String rightLabel;

    private DiffWindow(Dialog parent, String title, String leftLabel, String rightLabel) {
        super(parent, title, true); // modal dialog
        this.leftLabel = leftLabel;
        this.rightLabel = rightLabel;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Escape key closes dialog
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    public static DiffWindow create(Dialog parent, String title, String leftLabel, String rightLabel,
            String leftStrContent, String rightStrContent) {
        DiffWindow dd = new DiffWindow(parent, title, leftLabel, rightLabel);
        dd.buildContent(leftStrContent, rightStrContent);
        return dd;
    }

    public static DiffWindow create(String title, String leftLabel, String rightLabel,
            String leftStrContent, String rightStrContent) {
        DiffWindow dd = new DiffWindow(null, title, leftLabel, rightLabel);
        dd.buildContent(leftStrContent, rightStrContent);
        return dd;
    }

    public static DiffWindow createViewOnly(String title, String label, String content) {
        DiffWindow dd = new DiffWindow(null, title, label, null);
        dd.buildViewOnlyContent(content);
        return dd;
    }

    private void buildContent(String leftStrContent, String rightStrContent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createLabelPanel(), BorderLayout.NORTH);
        panel.add(new SimpleDiffPanel(leftStrContent, rightStrContent), BorderLayout.CENTER);
        add(panel);
    }

    private void buildViewOnlyContent(String content) {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel lbl = new JLabel(leftLabel, JLabel.CENTER);
        lbl.setFont(new Font(lbl.getFont().getName(), Font.BOLD, 14));
        JPanel labelPanel = new JPanel(new GridLayout(1, 1));
        labelPanel.add(lbl);

        panel.add(labelPanel, BorderLayout.NORTH);
        panel.add(new SimpleDiffPanel(content), BorderLayout.CENTER);
        add(panel);
    }

    private JPanel createLabelPanel() {
        JPanel labelPanel = new JPanel(new GridLayout(1, 2));

        JLabel lblLeft = new JLabel(leftLabel, JLabel.CENTER);
        Font labelFont = new Font(lblLeft.getFont().getName(), Font.BOLD, 14);
        lblLeft.setFont(labelFont);
        labelPanel.add(lblLeft);

        JLabel lblRight = new JLabel(rightLabel, JLabel.CENTER);
        lblRight.setFont(labelFont);
        labelPanel.add(lblRight);

        return labelPanel;
    }
}
