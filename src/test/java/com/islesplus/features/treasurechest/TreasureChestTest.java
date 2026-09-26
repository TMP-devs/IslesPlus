package com.islesplus.features.treasurechest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreasureChestTest {

    // ---- rarity from the model (as scanned 2026-09-24) -----------------------------------------

    @Test void rarityIsReadFromTheChestTopModel() {
        assertEquals("common", TreasureChest.rarityOf("modelengine", "prop_crate_common/chest_top"));
        assertEquals("rare", TreasureChest.rarityOf("modelengine", "prop_crate_rare/chest_top"));
    }

    @Test void aTierNotSeenYetIsStillAChest() {
        assertEquals("mythic", TreasureChest.rarityOf("modelengine", "prop_crate_mythic/chest_top"));
    }

    /** Each chest is two models on one spot; only the top counts, so a chest is one waypoint. */
    @Test void theChestBottomIsNotCountedTwice() {
        assertNull(TreasureChest.rarityOf("modelengine", "prop_crate_rare/chest_bot"));
    }

    @Test void otherModelsAreNotChests() {
        assertNull(TreasureChest.rarityOf("modelengine", "prop_waystone/totem"));
        assertNull(TreasureChest.rarityOf("isles", "prop_crate_rare/chest_top"));
        assertNull(TreasureChest.rarityOf("isles", "misc/gold_coin_display"));
    }

    // ---- which chests, from the label over them (scan 2026-09-24 05:23) ------------------------

    /** "TREASURE" chests are all over the isle, one by the cooking station - not the ones wanted
     * (scan 2026-09-24 19:22). */
    @Test void aTreasureLabelledChestGetsNoWaypoint() {
        assertNull(TreasureChest.nameFromLabel("TREASURE\n to loot!"));
    }

    @Test void aSunkenChestKeepsItsOwnName() {
        assertEquals("Sunken Tortuga Chest",
            TreasureChest.nameFromLabel("Sunken Tortuga Chest\nRequires a sunken key.\n to loot!"));
    }

    /** Sunken chests are not only Tortuga's: any place's name works. */
    @Test void aSunkenChestSomewhereElseIsOneToo() {
        assertEquals("Sunken Frog City Chest", TreasureChest.nameFromLabel("Sunken Frog City Chest\n to loot!"));
        assertEquals("Old Wreck Chest", TreasureChest.nameFromLabel("Old Wreck Chest\nRequires a sunken key."));
    }

    @Test void aLockedChestGetsNoWaypoint() {
        assertNull(TreasureChest.nameFromLabel("LOCKED\n2/5 Kills"));
    }

    @Test void anythingElseGetsNoWaypoint() {
        assertNull(TreasureChest.nameFromLabel(""));
        assertNull(TreasureChest.nameFromLabel("Frog City Waystone\nClick to open the Menu!"));
    }

    // ---- the tag -----------------------------------------------------------------------------

    @Test void distanceIsWholeMetres() {
        assertEquals("42m", TreasureChest.distance(42.4));
        assertEquals("43m", TreasureChest.distance(42.6));
    }

    @Test void commonIsGreenAndRareIsBlue() {
        assertEquals(TreasureChest.GREEN, TreasureChest.colorOf("common"));
        assertEquals(TreasureChest.BLUE, TreasureChest.colorOf("rare"));
        assertEquals(TreasureChest.LIGHT_BLUE, TreasureChest.colorOf("uncommon"));
        assertEquals(TreasureChest.PURPLE, TreasureChest.colorOf("epic"));
        assertEquals(TreasureChest.GOLD, TreasureChest.colorOf("legendary"));
    }

    @Test void anUnknownTierIsWhite() {
        assertEquals(TreasureChest.WHITE, TreasureChest.colorOf("mythic"));
    }

    // ---- hiding up close ---------------------------------------------------------------------

    @Test void theTagHidesWithinFiveBlocks() {
        assertFalse(TreasureChest.showTag(4.9));
        assertFalse(TreasureChest.showTag(5.0));
        assertTrue(TreasureChest.showTag(5.1));
    }

    // ---- Tortuga's chests are for divers ------------------------------------------------------

    @Test void tortugaChestsHideAboveY32() {
        assertTrue(TreasureChest.shownFromHeight("Sunken Tortuga Chest", 32.0));
        assertFalse(TreasureChest.shownFromHeight("Sunken Tortuga Chest", 32.1));
        assertFalse(TreasureChest.shownFromHeight("Sunken Tortuga Chest", 80.0));
        assertTrue(TreasureChest.shownFromHeight("Sunken Tortuga Chest", -10.0));
    }

    @Test void otherSunkenChestsShowAtAnyHeight() {
        assertTrue(TreasureChest.shownFromHeight("Sunken Frog City Chest", 80.0));
    }
}
