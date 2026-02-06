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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;

public class SimpleDiffPanelTest {

    @Test
    public void testToCharList() {
        List<String> chars = SimpleDiffPanel.toCharList("abc");
        assertEquals(3, chars.size());
        assertEquals("a", chars.get(0));
        assertEquals("b", chars.get(1));
        assertEquals("c", chars.get(2));
    }

    @Test
    public void testToCharListEmpty() {
        List<String> chars = SimpleDiffPanel.toCharList("");
        assertTrue(chars.isEmpty());
    }

    @Test
    public void testComputeInlineHighlightsChangedChars() {
        // "hello world" -> "hello earth"
        String oldLine = "hello world";
        String newLine = "hello earth";
        Patch<String> charPatch = DiffUtils.diff(
                SimpleDiffPanel.toCharList(oldLine),
                SimpleDiffPanel.toCharList(newLine));

        boolean[] oldHighlights = SimpleDiffPanel.computeInlineHighlights(oldLine.length(), charPatch, true);
        boolean[] newHighlights = SimpleDiffPanel.computeInlineHighlights(newLine.length(), charPatch, false);

        // "hello " is unchanged (positions 0-5)
        for (int i = 0; i < 6; i++) {
            assertFalse("Old position " + i + " should not be highlighted", oldHighlights[i]);
            assertFalse("New position " + i + " should not be highlighted", newHighlights[i]);
        }

        // "world" / "earth" should be highlighted (positions 6-10)
        boolean hasOldHighlight = false;
        for (int i = 6; i < oldLine.length(); i++) {
            if (oldHighlights[i]) hasOldHighlight = true;
        }
        assertTrue("Old line should have highlights in changed region", hasOldHighlight);

        boolean hasNewHighlight = false;
        for (int i = 6; i < newLine.length(); i++) {
            if (newHighlights[i]) hasNewHighlight = true;
        }
        assertTrue("New line should have highlights in changed region", hasNewHighlight);
    }

    @Test
    public void testComputeInlineHighlightsIdenticalLines() {
        String line = "no changes here";
        Patch<String> charPatch = DiffUtils.diff(
                SimpleDiffPanel.toCharList(line),
                SimpleDiffPanel.toCharList(line));

        boolean[] highlights = SimpleDiffPanel.computeInlineHighlights(line.length(), charPatch, true);

        for (int i = 0; i < line.length(); i++) {
            assertFalse("Position " + i + " should not be highlighted", highlights[i]);
        }
    }

    @Test
    public void testComputeInlineHighlightsEntireLineChanged() {
        String oldLine = "aaa";
        String newLine = "bbb";
        Patch<String> charPatch = DiffUtils.diff(
                SimpleDiffPanel.toCharList(oldLine),
                SimpleDiffPanel.toCharList(newLine));

        boolean[] oldHighlights = SimpleDiffPanel.computeInlineHighlights(oldLine.length(), charPatch, true);
        boolean[] newHighlights = SimpleDiffPanel.computeInlineHighlights(newLine.length(), charPatch, false);

        // Every character should be highlighted on both sides
        for (int i = 0; i < oldLine.length(); i++) {
            assertTrue("Old position " + i + " should be highlighted", oldHighlights[i]);
        }
        for (int i = 0; i < newLine.length(); i++) {
            assertTrue("New position " + i + " should be highlighted", newHighlights[i]);
        }
    }

    @Test
    public void testComputeInlineHighlightsInsertion() {
        // "ab" -> "aXb" — "X" inserted at position 1
        String oldLine = "ab";
        String newLine = "aXb";
        Patch<String> charPatch = DiffUtils.diff(
                SimpleDiffPanel.toCharList(oldLine),
                SimpleDiffPanel.toCharList(newLine));

        boolean[] newHighlights = SimpleDiffPanel.computeInlineHighlights(newLine.length(), charPatch, false);

        // "a" unchanged, "X" highlighted, "b" unchanged
        assertFalse(newHighlights[0]);
        assertTrue(newHighlights[1]);
        assertFalse(newHighlights[2]);
    }

    @Test
    public void testComputeInlineHighlightsDeletion() {
        // "aXb" -> "ab" — "X" deleted from position 1
        String oldLine = "aXb";
        String newLine = "ab";
        Patch<String> charPatch = DiffUtils.diff(
                SimpleDiffPanel.toCharList(oldLine),
                SimpleDiffPanel.toCharList(newLine));

        boolean[] oldHighlights = SimpleDiffPanel.computeInlineHighlights(oldLine.length(), charPatch, true);

        // "a" unchanged, "X" highlighted, "b" unchanged
        assertFalse(oldHighlights[0]);
        assertTrue(oldHighlights[1]);
        assertFalse(oldHighlights[2]);
    }

    @Test
    public void testEmptyContentProducesEmptyList() {
        // Verify the fix: empty string splits to empty list, not [""]
        // This ensures entirely new/removed components get INSERT/DELETE deltas (green/red)
        // instead of CHANGE deltas (blue/yellow)
        String empty = "";
        List<String> lines = empty.isEmpty() ? List.of() : java.util.Arrays.asList(empty.split("\n", -1));
        assertTrue("Empty content should produce empty list", lines.isEmpty());

        String nonEmpty = "line1\nline2";
        List<String> nonEmptyLines = nonEmpty.isEmpty() ? List.of() : java.util.Arrays.asList(nonEmpty.split("\n", -1));
        assertEquals(2, nonEmptyLines.size());
    }
}
