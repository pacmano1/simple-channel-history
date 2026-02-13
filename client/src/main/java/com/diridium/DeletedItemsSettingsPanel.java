// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthTable;
import com.mirth.connect.model.converters.ObjectXMLSerializer;

/**
 * Settings panel for viewing and managing deleted channel/code template snapshots.
 */
public class DeletedItemsSettingsPanel extends AbstractSettingsPanel {

    private static final Logger log = LoggerFactory.getLogger(DeletedItemsSettingsPanel.class);

    private static final String[] COLUMN_NAMES = {"Type", "Name", "Item ID", "Deleted By", "Date Deleted"};

    private static final String FILTER_ALL = "All";
    private static final String FILTER_CHANNELS = "Channels";
    private static final String FILTER_CODE_TEMPLATES = "Code Templates";

    private ChannelHistoryServletInterface servlet;
    private MirthTable table;
    private DeletedItemTableModel model;
    private List<DeletedItemInfo> allItems = Collections.emptyList();
    private JComboBox<String> filterCombo;
    private JButton btnViewXml;
    private JButton btnDiff;
    private JButton btnDownload;
    private JButton btnPurge;

    public DeletedItemsSettingsPanel(String tabName) {
        super(tabName);
        ObjectXMLSerializer.getInstance().allowTypes(
                Collections.emptyList(),
                Arrays.asList(DeletedItemInfo.class.getPackage().getName() + ".**"),
                Collections.emptyList());
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        table = new MirthTable();
        table.setHighlighters(HighlighterFactory.createAlternateStriping(
                UIConstants.HIGHLIGHTER_COLOR, UIConstants.BACKGROUND_COLOR));
        table.setSortable(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        model = new DeletedItemTableModel(Collections.emptyList());
        table.setModel(model);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRowCount() == 1) {
                    viewXml();
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(new JLabel("Show:"));
        filterCombo = new JComboBox<>(new String[]{FILTER_ALL, FILTER_CHANNELS, FILTER_CODE_TEMPLATES});
        filterCombo.addActionListener(e -> applyFilter());
        buttonPanel.add(filterCombo);

        btnViewXml = new JButton("View XML");
        btnViewXml.setEnabled(false);
        btnViewXml.addActionListener(e -> viewXml());
        buttonPanel.add(btnViewXml);

        btnDiff = new JButton("Show Diff");
        btnDiff.setEnabled(false);
        btnDiff.addActionListener(e -> showDiff());
        buttonPanel.add(btnDiff);

        btnDownload = new JButton("Download XML");
        btnDownload.setEnabled(false);
        btnDownload.addActionListener(e -> downloadXml());
        buttonPanel.add(btnDownload);

        btnPurge = new JButton("Purge");
        btnPurge.setEnabled(false);
        btnPurge.addActionListener(e -> purge());
        buttonPanel.add(btnPurge);

        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private ChannelHistoryServletInterface getServlet() {
        if (servlet == null) {
            try {
                servlet = PlatformUI.MIRTH_FRAME.mirthClient.getServlet(ChannelHistoryServletInterface.class);
            } catch (Exception e) {
                log.error("Failed to get servlet", e);
            }
        }
        return servlet;
    }

    @Override
    public void doRefresh() {
        new SwingWorker<List<DeletedItemInfo>, Void>() {
            @Override
            protected List<DeletedItemInfo> doInBackground() throws Exception {
                List<DeletedItemInfo> all = new ArrayList<>();
                ChannelHistoryServletInterface svc = getServlet();
                if (svc != null) {
                    try {
                        List<DeletedItemInfo> channels = svc.getDeletedChannels();
                        for (DeletedItemInfo info : channels) {
                            info.setType(DeletedItemInfo.TYPE_CHANNEL);
                        }
                        all.addAll(channels);
                    } catch (Exception e) {
                        log.error("Failed to load deleted channels", e);
                    }
                    try {
                        List<DeletedItemInfo> codeTemplates = svc.getDeletedCodeTemplates();
                        for (DeletedItemInfo info : codeTemplates) {
                            info.setType(DeletedItemInfo.TYPE_CODE_TEMPLATE);
                        }
                        all.addAll(codeTemplates);
                    } catch (Exception e) {
                        log.error("Failed to load deleted code templates", e);
                    }
                    // Sort newest-first
                    all.sort((a, b) -> Long.compare(b.getDateDeleted(), a.getDateDeleted()));
                }
                return all;
            }

            @Override
            protected void done() {
                try {
                    allItems = get();
                } catch (Exception e) {
                    log.error("Failed to refresh deleted items", e);
                    allItems = Collections.emptyList();
                }
                applyFilter();
            }
        }.execute();
    }

    private void applyFilter() {
        String filter = (String) filterCombo.getSelectedItem();
        List<DeletedItemInfo> filtered;
        if (FILTER_CHANNELS.equals(filter)) {
            filtered = new ArrayList<>();
            for (DeletedItemInfo info : allItems) {
                if (DeletedItemInfo.TYPE_CHANNEL.equals(info.getType())) filtered.add(info);
            }
        } else if (FILTER_CODE_TEMPLATES.equals(filter)) {
            filtered = new ArrayList<>();
            for (DeletedItemInfo info : allItems) {
                if (DeletedItemInfo.TYPE_CODE_TEMPLATE.equals(info.getType())) filtered.add(info);
            }
        } else {
            filtered = allItems;
        }
        model = new DeletedItemTableModel(filtered);
        table.setModel(model);
        btnViewXml.setEnabled(false);
        btnDiff.setEnabled(false);
        btnDownload.setEnabled(false);
        btnPurge.setEnabled(false);
    }

    @Override
    public boolean doSave() {
        // Nothing to configure — this panel is read-only
        return true;
    }

    private void updateButtonStates() {
        int count = table.getSelectedRowCount();
        btnViewXml.setEnabled(count == 1);
        btnDownload.setEnabled(count == 1);
        btnPurge.setEnabled(count == 1);

        // Diff enabled when exactly 2 rows selected with the same type
        boolean canDiff = false;
        if (count == 2) {
            int[] rows = table.getSelectedRows();
            DeletedItemInfo a = model.getItemAt(rows[0]);
            DeletedItemInfo b = model.getItemAt(rows[1]);
            canDiff = a.getType().equals(b.getType());
        }
        btnDiff.setEnabled(canDiff);
    }

    private void showDiff() {
        int[] rows = table.getSelectedRows();
        if (rows.length != 2) return;

        ChannelHistoryServletInterface svc = getServlet();
        if (svc == null) return;

        DeletedItemInfo a = model.getItemAt(rows[0]);
        DeletedItemInfo b = model.getItemAt(rows[1]);

        // Assign older/newer by date
        DeletedItemInfo older = a.getDateDeleted() <= b.getDateDeleted() ? a : b;
        DeletedItemInfo newer = a.getDateDeleted() <= b.getDateDeleted() ? b : a;

        try {
            String left;
            String right;
            boolean isChannel = DeletedItemInfo.TYPE_CHANNEL.equals(newer.getType());

            if (isChannel) {
                left = svc.getDeletedChannelContent(older.getId());
                right = svc.getDeletedChannelContent(newer.getId());
            } else {
                left = svc.getDeletedCodeTemplateContent(older.getId());
                right = svc.getDeletedCodeTemplateContent(newer.getId());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String leftLabel = "Old - " + older.getName() + " (deleted " + sdf.format(new Date(older.getDateDeleted())) + ")";
            String rightLabel = "New - " + newer.getName() + " (deleted " + sdf.format(new Date(newer.getDateDeleted())) + ")";
            String title = "Deleted " + newer.getType() + " Diff - " + newer.getName();

            if (isChannel) {
                try {
                    ChannelXmlDecomposer.DecomposeResult leftResult = ChannelXmlDecomposer.decomposeWithNames(left);
                    ChannelXmlDecomposer.DecomposeResult rightResult = ChannelXmlDecomposer.decomposeWithNames(right);
                    DecomposedDiffWindow dw = DecomposedDiffWindow.create(null, title,
                            leftLabel, rightLabel, leftResult, rightResult, left, right);
                    dw.setSize(PlatformUI.MIRTH_FRAME.getWidth() - 10, PlatformUI.MIRTH_FRAME.getHeight() - 10);
                    dw.setVisible(true);
                } catch (Exception decompositionEx) {
                    log.warn("Channel decomposition failed, falling back to raw diff: {}", decompositionEx.getMessage(), decompositionEx);
                    DiffWindow dw = DiffWindow.create(title, leftLabel, rightLabel, left, right);
                    dw.setSize(PlatformUI.MIRTH_FRAME.getWidth() - 10, PlatformUI.MIRTH_FRAME.getHeight() - 10);
                    dw.setVisible(true);
                }
            } else {
                DiffWindow dw = DiffWindow.create(title, leftLabel, rightLabel, left, right);
                dw.setSize(PlatformUI.MIRTH_FRAME.getWidth() - 10, PlatformUI.MIRTH_FRAME.getHeight() - 10);
                dw.setVisible(true);
            }
        } catch (Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }

    private void viewXml() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        ChannelHistoryServletInterface svc = getServlet();
        if (svc == null) return;

        DeletedItemInfo info = model.getItemAt(row);
        try {
            String xml;
            boolean isChannel = DeletedItemInfo.TYPE_CHANNEL.equals(info.getType());

            if (isChannel) {
                xml = svc.getDeletedChannelContent(info.getId());
            } else {
                xml = svc.getDeletedCodeTemplateContent(info.getId());
            }

            if (xml == null) {
                PlatformUI.MIRTH_FRAME.alertError(this, "No content found for this deleted item.");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String label = info.getName() + " (deleted " + sdf.format(new Date(info.getDateDeleted())) + ")";
            String title = "Deleted " + info.getType() + " - " + info.getName();

            if (isChannel) {
                try {
                    ChannelXmlDecomposer.DecomposeResult result = ChannelXmlDecomposer.decomposeWithNames(xml);
                    DecomposedDiffWindow dw = DecomposedDiffWindow.createViewOnly(null, title, label, result, xml);
                    dw.setSize(PlatformUI.MIRTH_FRAME.getWidth() - 10, PlatformUI.MIRTH_FRAME.getHeight() - 10);
                    dw.setVisible(true);
                } catch (Exception decompositionEx) {
                    log.warn("Channel decomposition failed, falling back to raw view: {}", decompositionEx.getMessage(), decompositionEx);
                    DiffWindow dw = DiffWindow.createViewOnly(title, label, xml);
                    dw.setSize(PlatformUI.MIRTH_FRAME.getWidth() - 10, PlatformUI.MIRTH_FRAME.getHeight() - 10);
                    dw.setVisible(true);
                }
            } else {
                DiffWindow dw = DiffWindow.createViewOnly(title, label, xml);
                dw.setSize(PlatformUI.MIRTH_FRAME.getWidth() - 10, PlatformUI.MIRTH_FRAME.getHeight() - 10);
                dw.setVisible(true);
            }
        } catch (Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }

    private void downloadXml() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        ChannelHistoryServletInterface svc = getServlet();
        if (svc == null) return;

        DeletedItemInfo info = model.getItemAt(row);
        try {
            String xml;
            if (DeletedItemInfo.TYPE_CHANNEL.equals(info.getType())) {
                xml = svc.getDeletedChannelContent(info.getId());
            } else {
                xml = svc.getDeletedCodeTemplateContent(info.getId());
            }

            if (xml == null) {
                PlatformUI.MIRTH_FRAME.alertError(this, "No content found for this deleted item.");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(info.getName() + ".xml"));
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(xml);
                    PlatformUI.MIRTH_FRAME.alertInformation(this, "XML saved to " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }

    private void purge() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        DeletedItemInfo info = model.getItemAt(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete the snapshot for \"" + info.getName() + "\"?\nThis action cannot be undone.",
                "Confirm Purge",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        ChannelHistoryServletInterface svc = getServlet();
        if (svc == null) return;

        try {
            if (DeletedItemInfo.TYPE_CHANNEL.equals(info.getType())) {
                svc.purgeDeletedChannel(info.getId());
            } else {
                svc.purgeDeletedCodeTemplate(info.getId());
            }
            doRefresh();
        } catch (Exception e) {
            PlatformUI.MIRTH_FRAME.alertThrowable(PlatformUI.MIRTH_FRAME, e);
        }
    }

    // ========== Table Model ==========

    static class DeletedItemTableModel extends AbstractTableModel {

        private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        private final List<DeletedItemInfo> items;

        DeletedItemTableModel(List<DeletedItemInfo> items) {
            this.items = items;
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DeletedItemInfo info = items.get(rowIndex);
            switch (columnIndex) {
                case 0: return info.getType();
                case 1: return info.getName();
                case 2: return info.getItemId();
                case 3: return info.getDeletedBy();
                case 4: return df.format(new Date(info.getDateDeleted()));
                default: throw new IllegalArgumentException("unknown column " + columnIndex);
            }
        }

        DeletedItemInfo getItemAt(int row) {
            return items.get(row);
        }
    }
}
