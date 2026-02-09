// SPDX-FileCopyrightText: Copyright 2025-2026 Diridium Technologies Inc.
// SPDX-License-Identifier: MPL-2.0

package com.diridium;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ComponentTreePanelTest {

    @Test
    public void testAllUnchanged() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("a", ComponentTreePanel.ChangeType.UNCHANGED);
        changeTypes.put("b", ComponentTreePanel.ChangeType.UNCHANGED);

        assertEquals(ComponentTreePanel.ChangeType.UNCHANGED,
                ComponentTreePanel.computeGroupChangeType(changeTypes, Arrays.asList("a", "b")));
    }

    @Test
    public void testAllLeftOnly() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("a", ComponentTreePanel.ChangeType.LEFT_ONLY);
        changeTypes.put("b", ComponentTreePanel.ChangeType.LEFT_ONLY);

        assertEquals(ComponentTreePanel.ChangeType.LEFT_ONLY,
                ComponentTreePanel.computeGroupChangeType(changeTypes, Arrays.asList("a", "b")));
    }

    @Test
    public void testAllRightOnly() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("a", ComponentTreePanel.ChangeType.RIGHT_ONLY);
        changeTypes.put("b", ComponentTreePanel.ChangeType.RIGHT_ONLY);

        assertEquals(ComponentTreePanel.ChangeType.RIGHT_ONLY,
                ComponentTreePanel.computeGroupChangeType(changeTypes, Arrays.asList("a", "b")));
    }

    @Test
    public void testAllModified() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("a", ComponentTreePanel.ChangeType.MODIFIED);

        assertEquals(ComponentTreePanel.ChangeType.MODIFIED,
                ComponentTreePanel.computeGroupChangeType(changeTypes, List.of("a")));
    }

    @Test
    public void testMixedChangesRollUpToModified() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("a", ComponentTreePanel.ChangeType.UNCHANGED);
        changeTypes.put("b", ComponentTreePanel.ChangeType.RIGHT_ONLY);

        // Mix of unchanged and right_only → MODIFIED
        assertEquals(ComponentTreePanel.ChangeType.MODIFIED,
                ComponentTreePanel.computeGroupChangeType(changeTypes, Arrays.asList("a", "b")));
    }

    @Test
    public void testLeftOnlyAndRightOnlyRollUpToModified() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("a", ComponentTreePanel.ChangeType.LEFT_ONLY);
        changeTypes.put("b", ComponentTreePanel.ChangeType.RIGHT_ONLY);

        assertEquals(ComponentTreePanel.ChangeType.MODIFIED,
                ComponentTreePanel.computeGroupChangeType(changeTypes, Arrays.asList("a", "b")));
    }

    @Test
    public void testModifiedWithUnchangedRollsUpToModified() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("a", ComponentTreePanel.ChangeType.MODIFIED);
        changeTypes.put("b", ComponentTreePanel.ChangeType.UNCHANGED);
        changeTypes.put("c", ComponentTreePanel.ChangeType.UNCHANGED);

        assertEquals(ComponentTreePanel.ChangeType.MODIFIED,
                ComponentTreePanel.computeGroupChangeType(changeTypes, Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testMissingKeyTreatedAsUnchanged() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        // "a" not in the map — should default to UNCHANGED

        assertEquals(ComponentTreePanel.ChangeType.UNCHANGED,
                ComponentTreePanel.computeGroupChangeType(changeTypes, List.of("a")));
    }

    @Test
    public void testEmptyKeyList() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();

        assertEquals(ComponentTreePanel.ChangeType.UNCHANGED,
                ComponentTreePanel.computeGroupChangeType(changeTypes, List.of()));
    }

    @Test
    public void testSingleLeftOnlyChild() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("removed", ComponentTreePanel.ChangeType.LEFT_ONLY);

        assertEquals(ComponentTreePanel.ChangeType.LEFT_ONLY,
                ComponentTreePanel.computeGroupChangeType(changeTypes, List.of("removed")));
    }

    @Test
    public void testSingleRightOnlyChild() {
        Map<String, ComponentTreePanel.ChangeType> changeTypes = new LinkedHashMap<>();
        changeTypes.put("added", ComponentTreePanel.ChangeType.RIGHT_ONLY);

        assertEquals(ComponentTreePanel.ChangeType.RIGHT_ONLY,
                ComponentTreePanel.computeGroupChangeType(changeTypes, List.of("added")));
    }
}
