package com.islesplus.features.storagecount;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageCountTest {
    // ---- the amount, off the tooltip (scan 2026-09-24 21:25) ------------------------------------

    @Test void theStoredLineIsTheAmount() {
        assertEquals(580, StorageCount.stored(List.of("Material", "", "Stored: 580/2048", "Click to withdraw")));
    }

    @Test void thousandsMayHaveCommas() {
        assertEquals(12_345, StorageCount.stored(List.of("  Stored: 12,345/20,480")));
    }

    @Test void noStoredLineNoAmount() {
        assertEquals(-1, StorageCount.stored(List.of("Honing Stone", "Used to hone gear")));
        assertEquals(-1, StorageCount.stored(List.of()));
    }

    // ---- how it reads in the slot ---------------------------------------------------------------

    @Test void underAThousandExact() {
        assertEquals("580", StorageCount.label(580));
        assertEquals("999", StorageCount.label(999));
        assertEquals("1", StorageCount.label(1));
    }

    /** Rounded down, so it never says more than there is; ".0" left off. */
    @Test void thousandsShortened() {
        assertEquals("1k", StorageCount.label(1_000));
        assertEquals("1.2k", StorageCount.label(1_250));
        assertEquals("1.9k", StorageCount.label(1_999));
        assertEquals("2k", StorageCount.label(2_048));
        assertEquals("12k", StorageCount.label(12_999));
        assertEquals("999k", StorageCount.label(999_999));
    }

    // ---- how big it is drawn: the game's font, whole screen pixels, inside the slot -------------

    /** Two digits fit at full size, as vanilla draws them. */
    @Test void aShortAmountIsFullSize() {
        assertEquals(1f, StorageCount.fitScale(12, 2, 16));
        assertEquals(1f, StorageCount.fitScale(12, 3, 16));
    }

    /** Three vanilla digits are 17 px, one more than the slot: the next crisp size down. */
    @Test void aLongAmountStepsDownAWholePixel() {
        assertEquals(0.5f, StorageCount.fitScale(17, 2, 16));
        assertEquals(2f / 3f, StorageCount.fitScale(17, 3, 16));
        assertEquals(3f / 4f, StorageCount.fitScale(17, 4, 16));
    }

    /** Never smaller than one screen pixel per font pixel. */
    @Test void neverBelowOnePixel() {
        assertEquals(0.5f, StorageCount.fitScale(80, 2, 16));
        assertEquals(1f, StorageCount.fitScale(80, 1, 16));
    }

    @Test void millionsShortened() {
        assertEquals("1m", StorageCount.label(1_000_000));
        assertEquals("1.5m", StorageCount.label(1_500_000));
    }
}
