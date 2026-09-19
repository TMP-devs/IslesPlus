package com.islesplus.screen.islesscreen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
