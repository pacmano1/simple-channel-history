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

import java.awt.*;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
    }

    public static DiffWindow create(Dialog parent, String title, String leftLabel, String rightLabel,
            Object leftObj, Object rightObj, String leftStrContent, String rightStrContent) {
        DiffWindow dd = new DiffWindow(parent, title, leftLabel, rightLabel);
        dd.buildContent(leftStrContent, rightStrContent);
        return dd;
    }

    // Keep old signature for backwards compatibility (non-modal)
    public static DiffWindow create(String title, String leftLabel, String rightLabel,
            Object leftObj, Object rightObj, String leftStrContent, String rightStrContent) {
        DiffWindow dd = new DiffWindow(null, title, leftLabel, rightLabel);
        dd.buildContent(leftStrContent, rightStrContent);
        return dd;
    }

    private void buildContent(String leftStrContent, String rightStrContent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createLabelPanel(), BorderLayout.NORTH);
        panel.add(new SimpleDiffPanel(leftStrContent, rightStrContent), BorderLayout.CENTER);
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
