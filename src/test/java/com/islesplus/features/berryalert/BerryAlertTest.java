package com.islesplus.features.berryalert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BerryAlertTest {

    @Test void theHeadAndItsClickBoxShareASpot() {
        // scan 2: item_display and interaction both at 356.96 7.68 1915.25
        assertTrue(BerryAlert.sameSpot(356.96, 7.68, 1915.25, 356.96, 7.68, 1915.25));
        assertTrue(BerryAlert.sameSpot(356.96, 7.68, 1915.25, 356.965, 7.675, 1915.255));
    }

    @Test void aClickBoxElsewhereIsNotTheBerrys() {
        assertFalse(BerryAlert.sameSpot(356.96, 7.68, 1915.25, 356.96, 8.68, 1915.25));
        assertFalse(BerryAlert.sameSpot(356.96, 7.68, 1915.25, 357.96, 7.68, 1915.25));
    }
}
