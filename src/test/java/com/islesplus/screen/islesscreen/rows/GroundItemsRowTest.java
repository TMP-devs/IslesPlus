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
        assertEquals("Paused", GroundItemsRow.statusText(item(false, true, true, true, true)));
    }

    @Test void enabledWithNoAlertsShowsZero() {
        assertEquals("Watching · 0 alerts", GroundItemsRow.statusText(item(true, false, false, false, false)));
    }

    @Test void enabledWithOneAlertUsesSingular() {
        assertEquals("Watching · 1 alert", GroundItemsRow.statusText(item(true, true, false, false, false)));
    }

    @Test void enabledWithAllFourAlertsUsesPlural() {
        assertEquals("Watching · 4 alerts", GroundItemsRow.statusText(item(true, true, true, true, true)));
    }

    @Test void countsOnlyTrueFlags() {
        assertEquals("Watching · 2 alerts", GroundItemsRow.statusText(item(true, true, false, true, false)));
    }
}
