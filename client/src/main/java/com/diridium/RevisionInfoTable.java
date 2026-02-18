// SPDX-FileCopyrightText: Copyright 2024 Kiran Ayyagari
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import org.jdesktop.swingx.decorator.HighlighterFactory;

import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthTable;

/**
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class RevisionInfoTable extends MirthTable {

    public RevisionInfoTable() {
        super();
        setHighlighters(HighlighterFactory.createAlternateStriping(UIConstants.HIGHLIGHTER_COLOR, UIConstants.BACKGROUND_COLOR));
        setSortable(false);
    }
}
