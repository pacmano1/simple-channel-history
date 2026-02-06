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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.model.codetemplates.CodeTemplate;
import com.mirth.connect.model.converters.ObjectXMLSerializer;

/**
 * Dialog for viewing code template version history.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class CodeTemplateHistoryDialog extends JDialog {

    private String codeTemplateId;
    private String codeTemplateName;
    private RevisionInfoTable tblRevisions;
    private ChannelHistoryServletInterface servlet;
    private ObjectXMLSerializer serializer;
    private JButton btnShowDiff;
    private JButton btnRevert;
    private JButton btnClose;
    private JPopupMenu popupMenu;
    private JMenuItem menuPrune;

    public CodeTemplateHistoryDialog(JFrame parent, String codeTemplateId, String codeTemplateName) {
        super(parent, "Version History - " + codeTemplateName, true);
        this.codeTemplateId = codeTemplateId;
        this.codeTemplateName = codeTemplateName;
        this.serializer = ObjectXMLSerializer.getInstance();
        this.serializer.allowTypes(Collections.emptyList(), Arrays.asList(RevisionInfo.class.getPackage().getName() + ".**"), Collections.emptyList());

        initComponents();
        loadHistory();

        setSize(700, 400);
        setLocationRelativeTo(parent);

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

    private void initComponents() {
        setLayout(new BorderLayout());

        // Table
        tblRevisions = new RevisionInfoTable();
        tblRevisions.setRowSelectionAllowed(true);
        tblRevisions.setColumnSelectionAllowed(false);
        tblRevisions.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        tblRevisions.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Setup popup menu
        popupMenu = new JPopupMenu();
        menuPrune = new JMenuItem("Prune older revisions");
        menuPrune.addActionListener(e -> pruneOlderRevisions());
        popupMenu.add(menuPrune);

        tblRevisions.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblRevisions.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        compareWithPrevious(row);
                    }
                }
            }

            private void handlePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = tblRevisions.rowAtPoint(e.getPoint());
                    if (row >= 0 && !tblRevisions.isRowSelected(row)) {
                        tblRevisions.setRowSelectionInterval(row, row);
                    }
                    // Enable prune if a single row is selected and there are older revisions to delete
                    int selectedRow = tblRevisions.getSelectedRow();
                    int totalRows = tblRevisions.getRowCount();
                    boolean hasOlderRevisions = selectedRow < totalRows - 1;
                    menuPrune.setEnabled(tblRevisions.getSelectedRowCount() == 1 && hasOlderRevisions);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblRevisions);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with help and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Help icon with tooltip and click handler
        JLabel helpLabel = new JLabel(new ImageIcon(com.mirth.connect.client.ui.Frame.class.getResource("images/help.png")));
        String helpText = "<html>" +
                "<b>Double-click</b> a revision to compare with previous<br>" +
                "<b>Ctrl/Cmd-click</b> to select two revisions, then Show Diff<br>" +
                "<b>Right-click</b> for prune option" +
                "</html>";
        helpLabel.setToolTipText(helpText);
        helpLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        helpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(CodeTemplateHistoryDialog.this,
                        helpText,
                        "Help",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        helpPanel.add(helpLabel);
        bottomPanel.add(helpPanel, BorderLayout.WEST);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnShowDiff = new JButton("Show Diff");
        btnShowDiff.setEnabled(false);
        btnShowDiff.addActionListener(e -> showDiff());
        buttonPanel.add(btnShowDiff);

        btnRevert = new JButton("Revert to Selected");
        btnRevert.setEnabled(false);
        btnRevert.addActionListener(e -> revertToSelected());
        buttonPanel.add(btnRevert);

        btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());
        buttonPanel.add(btnClose);

        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateButtonStates() {
        int selectedCount = tblRevisions.getSelectedRowCount();
        btnShowDiff.setEnabled(selectedCount == 2);
        // Can only revert if one row is selected and it's not the latest (row 0)
        btnRevert.setEnabled(selectedCount == 1 && tblRevisions.getSelectedRow() > 0);
    }

    private void loadHistory() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (servlet == null) {
                    servlet = PlatformUI.MIRTH_FRAME.mirthClient.getServlet(ChannelHistoryServletInterface.class);
                }

                List<RevisionInfo> revisions = servlet.getCodeTemplateHistory(codeTemplateId);
                RevisionInfoTableModel model = new RevisionInfoTableModel(revisions);
                tblRevisions.setModel(model);
                updateButtonStates();
                toFront();
                requestFocus();
            } catch (Exception e) {
                PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
            }
        });
    }

    private void showDiff() {
        int[] rows = tblRevisions.getSelectedRows();
        if (rows.length != 2) {
            return;
        }

        RevisionInfoTableModel model = (RevisionInfoTableModel) tblRevisions.getModel();
        // rows[0] = newer (lower row index, table is newest-first), rows[1] = older
        // Assign left = older, right = newer to match standard diff convention
        RevisionInfo older = model.getRevisionAt(rows[1]);
        RevisionInfo newer = model.getRevisionAt(rows[0]);

        try {
            String left = servlet.getCodeTemplateContent(codeTemplateId, older.getHash());
            CodeTemplate leftCt = serializer.deserialize(left, CodeTemplate.class);

            String right = servlet.getCodeTemplateContent(codeTemplateId, newer.getHash());
            CodeTemplate rightCt = serializer.deserialize(right, CodeTemplate.class);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String leftLabel = String.format("Old - %s (user: %s, time: %s)", older.getShortHash(), older.getCommitterName(), sdf.format(new Date(older.getTime())));
            String rightLabel = String.format("New - %s (user: %s, time: %s)", newer.getShortHash(), newer.getCommitterName(), sdf.format(new Date(newer.getTime())));

            DiffWindow dw = DiffWindow.create(this, "Code Template Diff - " + codeTemplateName, leftLabel, rightLabel, leftCt, rightCt, left, right);
            dw.setSize(PlatformUI.MIRTH_FRAME.getWidth() - 10, PlatformUI.MIRTH_FRAME.getHeight() - 10);
            dw.setVisible(true);
        } catch (Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }

    private void compareWithPrevious(int row) {
        int totalRows = tblRevisions.getRowCount();
        if (row >= totalRows - 1) {
            JOptionPane.showMessageDialog(this,
                    "No previous revision to compare to.",
                    "Compare",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Select the clicked row and the next row (previous revision)
        tblRevisions.setRowSelectionInterval(row, row);
        tblRevisions.addRowSelectionInterval(row + 1, row + 1);
        showDiff();

        // Restore selection to just the originally clicked row
        tblRevisions.setRowSelectionInterval(row, row);
    }

    private void revertToSelected() {
        int row = tblRevisions.getSelectedRow();
        if (row <= 0) {
            return;
        }

        RevisionInfoTableModel model = (RevisionInfoTableModel) tblRevisions.getModel();
        RevisionInfo targetRevision = model.getRevisionAt(row);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to revert to revision " + targetRevision.getShortHash() + "?\n" +
                "This will overwrite the current code template.",
                "Confirm Revert",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean reverted = servlet.revertCodeTemplate(codeTemplateId, targetRevision.getHash());
            if (reverted) {
                PlatformUI.MIRTH_FRAME.alertInformation(this, "Code template reverted successfully.");
                // Refresh the code template panel
                PlatformUI.MIRTH_FRAME.codeTemplatePanel.doRefreshCodeTemplates(true);
                loadHistory();
            }
        } catch (Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }

    private void pruneOlderRevisions() {
        int row = tblRevisions.getSelectedRow();
        if (row < 0) {
            return;
        }

        RevisionInfoTableModel model = (RevisionInfoTableModel) tblRevisions.getModel();
        RevisionInfo selectedRevision = model.getRevisionAt(row);
        int revisionsToDelete = model.getRowCount() - row - 1;

        if (revisionsToDelete <= 0) {
            JOptionPane.showMessageDialog(this, "No older revisions to delete.", "Prune", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + revisionsToDelete + " revision(s) older than " + selectedRevision.getShortHash() + "?\n" +
                "This action cannot be undone.",
                "Confirm Prune",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int deleted = servlet.pruneCodeTemplateHistory(codeTemplateId, selectedRevision.getHash());
            PlatformUI.MIRTH_FRAME.alertInformation(this, "Deleted " + deleted + " older revision(s).");
            loadHistory();
        } catch (Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }
}
