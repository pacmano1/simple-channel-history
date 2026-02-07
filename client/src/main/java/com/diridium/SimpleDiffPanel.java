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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

/**
 * A simple side-by-side diff panel using java-diff-utils.
 *
 * @author Kiran Ayyagari (kayyagari@apache.org)
 */
public class SimpleDiffPanel extends JPanel {

    private static final Color COLOR_ADDED = new Color(200, 255, 200);      // Light green
    private static final Color COLOR_DELETED = new Color(255, 200, 200);    // Light red
    private static final Color COLOR_CHANGED_OLD = COLOR_DELETED;           // Changed old side = light red
    private static final Color COLOR_CHANGED_NEW = COLOR_ADDED;             // Changed new side = light green
    private static final Color COLOR_HIGHLIGHT_OLD = new Color(255, 150, 150); // Darker red for changed words
    private static final Color COLOR_HIGHLIGHT_NEW = new Color(130, 220, 130); // Darker green for changed words
    private static final Color COLOR_LINE_NUMBER_BG = new Color(240, 240, 240); // Light gray

    private JTextPane leftPane;
    private JTextPane rightPane;
    private JTextArea leftLineNumbers;
    private JTextArea rightLineNumbers;
    private JScrollPane leftScrollPane;
    private JScrollPane rightScrollPane;
    private boolean syncingScroll = false;

    private StringBuilder leftLineNumBuilder = new StringBuilder();
    private StringBuilder rightLineNumBuilder = new StringBuilder();

    public SimpleDiffPanel(String leftContent, String rightContent) {
        setLayout(new BorderLayout());

        // Create text panes
        leftPane = createTextPane();
        rightPane = createTextPane();

        // Create line number components
        leftLineNumbers = createLineNumberArea();
        rightLineNumbers = createLineNumberArea();

        // Create scroll panes with line numbers as row headers.
        // Use fixed preferred/minimum sizes so content doesn't drive layout.
        Dimension smallSize = new Dimension(0, 0);

        leftScrollPane = new JScrollPane(leftPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        leftScrollPane.setRowHeaderView(leftLineNumbers);
        leftScrollPane.setMinimumSize(smallSize);
        leftScrollPane.setPreferredSize(smallSize);

        rightScrollPane = new JScrollPane(rightPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightScrollPane.setRowHeaderView(rightLineNumbers);
        rightScrollPane.setMinimumSize(smallSize);
        rightScrollPane.setPreferredSize(smallSize);

        // Synchronize scrolling
        setupScrollSync();

        // Create split pane — resizeWeight keeps it 50/50 on resize
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScrollPane, rightScrollPane);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);

        // Compute and display diff
        displayDiff(leftContent, rightContent);
    }

    private JTextPane createTextPane() {
        // Override to disable word wrap — lets long lines extend beyond the viewport
        JTextPane pane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                if (getParent() == null || getUI() == null) {
                    return false;
                }
                return getUI().getPreferredSize(this).width <= getParent().getSize().width;
            }
        };
        pane.setEditable(false);
        pane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return pane;
    }

    private JTextArea createLineNumberArea() {
        JTextArea lineNumbers = new JTextArea();
        lineNumbers.setEditable(false);
        lineNumbers.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lineNumbers.setBackground(COLOR_LINE_NUMBER_BG);
        lineNumbers.setForeground(Color.GRAY);
        return lineNumbers;
    }

    private void setupScrollSync() {
        // Sync vertical scrolling
        leftScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!syncingScroll) {
                    syncingScroll = true;
                    rightScrollPane.getVerticalScrollBar().setValue(e.getValue());
                    syncingScroll = false;
                }
            }
        });

        rightScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!syncingScroll) {
                    syncingScroll = true;
                    leftScrollPane.getVerticalScrollBar().setValue(e.getValue());
                    syncingScroll = false;
                }
            }
        });

        // Sync horizontal scrolling
        leftScrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!syncingScroll) {
                    syncingScroll = true;
                    rightScrollPane.getHorizontalScrollBar().setValue(e.getValue());
                    syncingScroll = false;
                }
            }
        });

        rightScrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!syncingScroll) {
                    syncingScroll = true;
                    leftScrollPane.getHorizontalScrollBar().setValue(e.getValue());
                    syncingScroll = false;
                }
            }
        });
    }

    private void displayDiff(String leftContent, String rightContent) {
        List<String> leftLines = leftContent.isEmpty() ? List.of() : Arrays.asList(leftContent.split("\n", -1));
        List<String> rightLines = rightContent.isEmpty() ? List.of() : Arrays.asList(rightContent.split("\n", -1));

        Patch<String> patch = DiffUtils.diff(leftLines, rightLines);

        StyledDocument leftDoc = leftPane.getStyledDocument();
        StyledDocument rightDoc = rightPane.getStyledDocument();

        // Create styles
        Style normalStyle = leftDoc.addStyle("normal", null);

        Style addedStyle = leftDoc.addStyle("added", null);
        StyleConstants.setBackground(addedStyle, COLOR_ADDED);

        Style deletedStyle = leftDoc.addStyle("deleted", null);
        StyleConstants.setBackground(deletedStyle, COLOR_DELETED);

        Style changedOldStyle = leftDoc.addStyle("changedOld", null);
        StyleConstants.setBackground(changedOldStyle, COLOR_CHANGED_OLD);

        Style changedNewStyle = leftDoc.addStyle("changedNew", null);
        StyleConstants.setBackground(changedNewStyle, COLOR_CHANGED_NEW);

        Style highlightOldStyle = leftDoc.addStyle("highlightOld", null);
        StyleConstants.setBackground(highlightOldStyle, COLOR_HIGHLIGHT_OLD);

        // Also add styles to right doc
        rightDoc.addStyle("normal", null);
        Style rightAdded = rightDoc.addStyle("added", null);
        StyleConstants.setBackground(rightAdded, COLOR_ADDED);
        Style rightDeleted = rightDoc.addStyle("deleted", null);
        StyleConstants.setBackground(rightDeleted, COLOR_DELETED);
        Style rightChangedOld = rightDoc.addStyle("changedOld", null);
        StyleConstants.setBackground(rightChangedOld, COLOR_CHANGED_OLD);
        Style rightChangedNew = rightDoc.addStyle("changedNew", null);
        StyleConstants.setBackground(rightChangedNew, COLOR_CHANGED_NEW);

        Style highlightNewStyle = rightDoc.addStyle("highlightNew", null);
        StyleConstants.setBackground(highlightNewStyle, COLOR_HIGHLIGHT_NEW);

        try {
            // Track current position in each file
            int leftLine = 0;
            int rightLine = 0;

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                int sourceStart = delta.getSource().getPosition();
                int targetStart = delta.getTarget().getPosition();

                // Add unchanged lines before this delta
                while (leftLine < sourceStart) {
                    appendLine(leftDoc, leftLines.get(leftLine), normalStyle, leftLineNumBuilder, leftLine + 1);
                    leftLine++;
                }
                while (rightLine < targetStart) {
                    appendLine(rightDoc, rightLines.get(rightLine), normalStyle, rightLineNumBuilder, rightLine + 1);
                    rightLine++;
                }

                // Handle the delta based on type
                switch (delta.getType()) {
                    case DELETE:
                        // Lines deleted from left (not in right)
                        for (String line : delta.getSource().getLines()) {
                            appendLine(leftDoc, line, deletedStyle, leftLineNumBuilder, leftLine + 1);
                            leftLine++;
                        }
                        // Add blank lines on right to keep alignment (no line number)
                        for (int i = 0; i < delta.getSource().getLines().size(); i++) {
                            appendLine(rightDoc, "", deletedStyle, rightLineNumBuilder, -1);
                        }
                        break;

                    case INSERT:
                        // Lines added to right (not in left)
                        for (int i = 0; i < delta.getTarget().getLines().size(); i++) {
                            appendLine(leftDoc, "", addedStyle, leftLineNumBuilder, -1);
                        }
                        for (String line : delta.getTarget().getLines()) {
                            appendLine(rightDoc, line, addedStyle, rightLineNumBuilder, rightLine + 1);
                            rightLine++;
                        }
                        break;

                    case CHANGE:
                        // Lines changed - show old on left, new on right with word-level highlights
                        List<String> oldLines = delta.getSource().getLines();
                        List<String> newLines = delta.getTarget().getLines();
                        int maxLines = Math.max(oldLines.size(), newLines.size());

                        for (int i = 0; i < maxLines; i++) {
                            if (i < oldLines.size() && i < newLines.size()) {
                                // Both sides have a line — do inline character-level diff
                                String oldLine = oldLines.get(i);
                                String newLine = newLines.get(i);
                                Patch<String> charPatch = DiffUtils.diff(toCharList(oldLine), toCharList(newLine));

                                boolean[] oldHighlights = computeInlineHighlights(oldLine.length(), charPatch, true);
                                boolean[] newHighlights = computeInlineHighlights(newLine.length(), charPatch, false);

                                appendLineWithHighlights(leftDoc, oldLine, changedOldStyle, highlightOldStyle, oldHighlights, leftLineNumBuilder, leftLine + 1);
                                leftLine++;
                                appendLineWithHighlights(rightDoc, newLine, changedNewStyle, highlightNewStyle, newHighlights, rightLineNumBuilder, rightLine + 1);
                                rightLine++;
                            } else if (i < oldLines.size()) {
                                appendLine(leftDoc, oldLines.get(i), changedOldStyle, leftLineNumBuilder, leftLine + 1);
                                leftLine++;
                                appendLine(rightDoc, "", changedNewStyle, rightLineNumBuilder, -1);
                            } else {
                                appendLine(leftDoc, "", changedOldStyle, leftLineNumBuilder, -1);
                                appendLine(rightDoc, newLines.get(i), changedNewStyle, rightLineNumBuilder, rightLine + 1);
                                rightLine++;
                            }
                        }
                        break;

                    default:
                        break;
                }
            }

            // Add remaining unchanged lines
            while (leftLine < leftLines.size()) {
                appendLine(leftDoc, leftLines.get(leftLine), normalStyle, leftLineNumBuilder, leftLine + 1);
                leftLine++;
            }
            while (rightLine < rightLines.size()) {
                appendLine(rightDoc, rightLines.get(rightLine), normalStyle, rightLineNumBuilder, rightLine + 1);
                rightLine++;
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // Set line numbers text
        leftLineNumbers.setText(leftLineNumBuilder.toString());
        rightLineNumbers.setText(rightLineNumBuilder.toString());

        // Calculate width needed for line numbers
        int maxLineNum = Math.max(leftLines.size(), rightLines.size());
        int digits = String.valueOf(maxLineNum).length();
        FontMetrics fm = leftLineNumbers.getFontMetrics(leftLineNumbers.getFont());
        int width = fm.charWidth('0') * (digits + 2); // +2 for padding

        leftLineNumbers.setPreferredSize(new Dimension(width, leftLineNumbers.getPreferredSize().height));
        rightLineNumbers.setPreferredSize(new Dimension(width, rightLineNumbers.getPreferredSize().height));

        // Scroll to top
        leftPane.setCaretPosition(0);
        rightPane.setCaretPosition(0);
    }

    private void appendLine(StyledDocument doc, String line, Style style, StringBuilder lineNumBuilder, int lineNum) throws BadLocationException {
        int length = doc.getLength();
        if (length > 0) {
            doc.insertString(length, "\n", style);
            lineNumBuilder.append("\n");
        }
        doc.insertString(doc.getLength(), line, style);

        // Append line number (or blank for padding lines)
        if (lineNum > 0) {
            lineNumBuilder.append(String.format("%4d ", lineNum));
        } else {
            lineNumBuilder.append("     ");
        }
    }

    private void appendLineWithHighlights(StyledDocument doc, String line, Style baseStyle, Style highlightStyle,
            boolean[] highlights, StringBuilder lineNumBuilder, int lineNum) throws BadLocationException {
        int length = doc.getLength();
        if (length > 0) {
            doc.insertString(length, "\n", baseStyle);
            lineNumBuilder.append("\n");
        }

        // Insert the line in runs, switching between base and highlight styles
        int i = 0;
        while (i < line.length()) {
            boolean hl = (i < highlights.length) && highlights[i];
            int runStart = i;
            while (i < line.length()) {
                boolean nextHl = (i < highlights.length) && highlights[i];
                if (nextHl != hl) break;
                i++;
            }
            doc.insertString(doc.getLength(), line.substring(runStart, i), hl ? highlightStyle : baseStyle);
        }

        if (lineNum > 0) {
            lineNumBuilder.append(String.format("%4d ", lineNum));
        } else {
            lineNumBuilder.append("     ");
        }
    }

    static List<String> toCharList(String s) {
        List<String> chars = new ArrayList<>(s.length());
        for (char c : s.toCharArray()) {
            chars.add(String.valueOf(c));
        }
        return chars;
    }

    static boolean[] computeInlineHighlights(int lineLength, Patch<String> charPatch, boolean isSource) {
        boolean[] highlights = new boolean[lineLength];
        for (AbstractDelta<String> delta : charPatch.getDeltas()) {
            int pos;
            int size;
            if (isSource) {
                pos = delta.getSource().getPosition();
                size = delta.getSource().size();
            } else {
                pos = delta.getTarget().getPosition();
                size = delta.getTarget().size();
            }
            for (int i = pos; i < pos + size && i < lineLength; i++) {
                highlights[i] = true;
            }
        }
        return highlights;
    }
}
