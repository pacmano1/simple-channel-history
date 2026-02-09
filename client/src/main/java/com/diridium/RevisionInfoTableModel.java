// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class RevisionInfoTableModel extends AbstractTableModel {

    private List<RevisionInfo> revisions;

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String[] columnNames = {"Revision", "User", "Date"};

    public RevisionInfoTableModel(List<RevisionInfo> revisions) {
        this.revisions = revisions;
    }

    @Override
    public int getRowCount() {
        return revisions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object val = null;
        RevisionInfo r = revisions.get(rowIndex);

        switch (columnIndex) {
        case 0:
            val = r.getShortHash();
            break;

        case 1:
            val = r.getCommitterName();
            break;

        case 2:
            val = formatTime(r.getTime());
            break;

        default:
            throw new IllegalArgumentException("unknown column number " + columnIndex);
        }
        return val;
    }

    public RevisionInfo getRevisionAt(int row) {
        return revisions.get(row);
    }

    private String formatTime(long t) {
        long elapsed = System.currentTimeMillis() - t;
        if (elapsed < 0) {
            return df.format(new Date(t));
        }

        long seconds = elapsed / 1000;
        long minutes = seconds / 60;

        if (minutes >= 60) {
            return df.format(new Date(t));
        } else if (minutes > 0) {
            return minutes + " minutes ago";
        } else {
            return seconds + " seconds ago";
        }
    }
}
