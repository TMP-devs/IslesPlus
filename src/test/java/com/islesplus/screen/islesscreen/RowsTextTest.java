package com.islesplus.screen.islesscreen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowsTextTest {

    @Test void chatFilterSummaryReadsAsNothingHiddenAtZero() {
        assertEquals("Nothing hidden.", RowsText.chatFilterSummary(0));
    }

    @Test void chatFilterSummaryIsSingularForOne() {
        assertEquals("Hiding 1 type.", RowsText.chatFilterSummary(1));
    }

    @Test void chatFilterSummaryIsPluralForMoreThanOne() {
        assertEquals("Hiding 2 types.", RowsText.chatFilterSummary(2));
    }

    @Test void countChipJoinsWithSpacedSlash() {
        assertEquals("1 / 2", RowsText.countChip(1, 2));
        assertEquals("0 / 5", RowsText.countChip(0, 5));
    }

    @Test void searchMatchesEveryWordAnywhere() {
        assertTrue(RowsText.matchesSearch("Plushie Finder", "Highlight plushies.", ""));
        assertTrue(RowsText.matchesSearch("Plushie Finder", "Highlight plushies.", "plush"));
        assertTrue(RowsText.matchesSearch("Plushie Finder", "Highlight plushies.", "  HIGHLIGHT finder "));
        assertFalse(RowsText.matchesSearch("Plushie Finder", "Highlight plushies.", "plushie boss"));
        assertFalse(RowsText.matchesSearch("Boss Timers", null, "plushie"));
    }
}
