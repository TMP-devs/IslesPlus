package com.islesplus.screen.islesscreen.rows;

import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier.WatchedItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundItemsRowTest {

    private static WatchedItem item(boolean enabled, boolean line, boolean screen, boolean glow, boolean sound) {
        WatchedItem item = new WatchedItem();
        item.enabled = enabled;
        item.lineTracker = line;
        item.screenNotifier = screen;
        item.highlight = glow;
        item.soundPing = sound;
        return item;
    }

    @Test void disabledItemIsPaused() {
        assertEquals("PAUSED", GroundItemsRow.statusText(item(false, true, true, true, true)));
    }

    @Test void enabledWithNoAlertsShowsZero() {
        assertEquals("WATCHING · 0 ALERTS", GroundItemsRow.statusText(item(true, false, false, false, false)));
    }

    @Test void enabledWithOneAlertUsesSingular() {
        assertEquals("WATCHING · 1 ALERT", GroundItemsRow.statusText(item(true, true, false, false, false)));
    }

    @Test void enabledWithAllFourAlertsUsesPlural() {
        assertEquals("WATCHING · 4 ALERTS", GroundItemsRow.statusText(item(true, true, true, true, true)));
    }

    @Test void countsOnlyTrueFlags() {
        assertEquals("WATCHING · 2 ALERTS", GroundItemsRow.statusText(item(true, true, false, true, false)));
    }
}
