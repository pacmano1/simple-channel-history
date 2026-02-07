package com.diridium;

/*
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

public class DecomposedDiffWindow extends JDialog {

    private static final String VIEW_DECOMPOSED = "decomposed";
    private static final String VIEW_RAW = "raw";

    private final Map<String, DecomposedComponent> leftComponents;
    private final Map<String, DecomposedComponent> rightComponents;
    private final String leftRawXml;
    private final String rightRawXml;
    private JPanel diffContainer;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private JButton toggleButton;
    private boolean showingDecomposed = true;

    private DecomposedDiffWindow(java.awt.Dialog parent, String title, String leftLabel, String rightLabel,
                                  Map<String, DecomposedComponent> leftComponents,
                                  Map<String, DecomposedComponent> rightComponents,
                                  String leftRawXml, String rightRawXml) {
        super(parent, title, true);
        this.leftComponents = leftComponents;
        this.rightComponents = rightComponents;
        this.leftRawXml = leftRawXml;
        this.rightRawXml = rightRawXml;
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

        buildContent(leftLabel, rightLabel);
    }

    public static DecomposedDiffWindow create(java.awt.Dialog parent, String title,
            String leftLabel, String rightLabel,
            Map<String, DecomposedComponent> leftComponents,
            Map<String, DecomposedComponent> rightComponents,
            String leftRawXml, String rightRawXml) {
        return new DecomposedDiffWindow(parent, title, leftLabel, rightLabel,
                leftComponents, rightComponents, leftRawXml, rightRawXml);
    }

    private void buildContent(String leftLabel, String rightLabel) {
        // Revision labels — placed above the diff container so they align with left/right panes
        JPanel labelPanel = new JPanel(new GridLayout(1, 2));
        JLabel lblLeft = new JLabel(leftLabel, JLabel.CENTER);
        Font labelFont = new Font(lblLeft.getFont().getName(), Font.BOLD, 14);
        lblLeft.setFont(labelFont);
        labelPanel.add(lblLeft);

        JLabel lblRight = new JLabel(rightLabel, JLabel.CENTER);
        lblRight.setFont(labelFont);
        labelPanel.add(lblRight);

        // --- Decomposed view ---
        ComponentTreePanel treePanel = new ComponentTreePanel(leftComponents, rightComponents);
        treePanel.setPreferredSize(new Dimension(280, 0));
        treePanel.setMinimumSize(new Dimension(200, 0));

        JLabel summaryLabel = new JLabel(
                treePanel.getChangedCount() + " of " + treePanel.getTotalCount() + " components changed");
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.ITALIC));
        summaryLabel.setHorizontalAlignment(JLabel.CENTER);

        JPanel treeWithSummary = new JPanel(new BorderLayout());
        treeWithSummary.add(treePanel, BorderLayout.CENTER);
        treeWithSummary.add(summaryLabel, BorderLayout.SOUTH);

        // Diff container with labels above it so they align with left/right panes
        diffContainer = new JPanel(new BorderLayout());
        diffContainer.setMinimumSize(new Dimension(0, 0));

        JPanel diffWithLabels = new JPanel(new BorderLayout());
        diffWithLabels.add(labelPanel, BorderLayout.NORTH);
        diffWithLabels.add(diffContainer, BorderLayout.CENTER);

        JSplitPane decomposedSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeWithSummary, diffWithLabels);
        decomposedSplit.setDividerLocation(280);
        decomposedSplit.setOneTouchExpandable(true);

        // --- Raw XML view (created lazily on first toggle) ---
        // Use a CardLayout to swap between decomposed and raw views
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(decomposedSplit, VIEW_DECOMPOSED);

        // Toggle button
        toggleButton = new JButton("Show Raw XML");
        toggleButton.addActionListener(e -> toggleView());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(toggleButton, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Wire up selection listener
        treePanel.setComponentSelectionListener(key -> showComponentDiff(key));
    }

    private void toggleView() {
        if (showingDecomposed) {
            // Lazily create the raw view on first toggle
            if (cardPanel.getComponentCount() < 2) {
                SimpleDiffPanel rawDiff = new SimpleDiffPanel(leftRawXml, rightRawXml);
                cardPanel.add(rawDiff, VIEW_RAW);
            }
            cardLayout.show(cardPanel, VIEW_RAW);
            toggleButton.setText("Show Component View");
        } else {
            cardLayout.show(cardPanel, VIEW_DECOMPOSED);
            toggleButton.setText("Show Raw XML");
        }
        showingDecomposed = !showingDecomposed;
    }

    private void showComponentDiff(String key) {
        diffContainer.removeAll();

        String leftContent = "";
        String rightContent = "";

        DecomposedComponent leftComp = leftComponents.get(key);
        DecomposedComponent rightComp = rightComponents.get(key);

        if (leftComp != null) {
            leftContent = leftComp.getContent();
        }
        if (rightComp != null) {
            rightContent = rightComp.getContent();
        }

        diffContainer.add(new SimpleDiffPanel(leftContent, rightContent), BorderLayout.CENTER);
        diffContainer.revalidate();
        diffContainer.repaint();
    }
}
