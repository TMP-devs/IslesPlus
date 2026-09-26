package com.islesplus.features.rollpercent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RollCacheTest {
    private static final String HELMET = RollCache.item("Shadespore Helmet", "Lv.15");

    @Test void aLineSeenOnTheRealItemIsRememberedForItsStandIn() {
        RollCache cache = new RollCache(16);
        cache.remember(HELMET, "+8.9 Max Health", 0.72);
        assertEquals(0.72, cache.recall(HELMET, "+8.9 Max Health"));
    }

    @Test void aLineNeverSeenIsUnknown() {
        assertNull(new RollCache(16).recall(HELMET, "+8.9 Max Health"));
    }

    @Test void theSameLineOnAnotherItemIsADifferentRoll() {
        RollCache cache = new RollCache(16);
        cache.remember(HELMET, "+8.9 Max Health", 0.72);
        assertNull(cache.recall(RollCache.item("Shadespore Boots", "Lv.15"), "+8.9 Max Health"));
    }

    @Test void theSameItemAtAnotherLevelIsADifferentRoll() {
        RollCache cache = new RollCache(16);
        cache.remember(HELMET, "+8.9 Max Health", 0.72);
        assertNull(cache.recall(RollCache.item("Shadespore Helmet", "Lv.16"), "+8.9 Max Health"));
    }

    @Test void paddingAroundALineDoesNotMatter() {
        RollCache cache = new RollCache(16);
        cache.remember(HELMET, "   +8.9 Max Health ", 0.72);
        assertEquals(0.72, cache.recall(HELMET, "+8.9 Max Health"));
    }

    @Test void theOldestLinesAreDroppedWhenFull() {
        RollCache cache = new RollCache(2);
        cache.remember(HELMET, "a", 0.1);
        cache.remember(HELMET, "b", 0.2);
        cache.remember(HELMET, "c", 0.3);
        assertNull(cache.recall(HELMET, "a"));
        assertEquals(0.3, cache.recall(HELMET, "c"));
    }

    @Test void onlyANewOrChangedRollMakesItDirty() {
        RollCache cache = new RollCache(16);
        assertFalse(cache.dirty());
        cache.remember(HELMET, "+8.9 Max Health", 0.72);
        assertTrue(cache.dirty());
        cache.markSaved();
        cache.remember(HELMET, "+8.9 Max Health", 0.72);
        assertFalse(cache.dirty());
    }

    @Test void itSurvivesARoundTripThroughJson() {
        RollCache cache = new RollCache(16);
        cache.remember(HELMET, "+8.9 Max Health", 0.72);
        cache.remember(HELMET, "+4.8% Crit Chance", 0.35);
        RollCache loaded = new RollCache(16);
        loaded.readJson(cache.toJson());
        assertEquals(0.72, loaded.recall(HELMET, "+8.9 Max Health"));
        assertEquals(0.35, loaded.recall(HELMET, "+4.8% Crit Chance"));
        assertFalse(loaded.dirty());
    }

    @Test void brokenJsonLoadsAsEmpty() {
        RollCache cache = new RollCache(16);
        cache.readJson("{not json");
        cache.readJson("[1,2]");
        assertNull(cache.recall(HELMET, "+8.9 Max Health"));
    }
}
