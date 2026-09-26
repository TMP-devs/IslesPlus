package com.islesplus.features.harvestables;

import com.islesplus.features.harvestables.HarvestableHighlighter.Harvestable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Shapes from the 2026-09-24 scans. */
class HarvestableClassifyTest {
    @Test void scannedItemDisplays() {
        assertEquals(Harvestable.PLAIN_FIBER, HarvestableHighlighter.classifyItem("material_plain_fiber", "isles:mob_drops/string/plain_string"));
        assertEquals(Harvestable.COARSE_FIBER, HarvestableHighlighter.classifyItem("material_fiber", "isles:mob_drops/string/coarse_string"));
        assertEquals(Harvestable.WHISPERLEAF, HarvestableHighlighter.classifyItem("material_whisperleaf", "isles:mob_drops/whisperleaf"));
        assertEquals(Harvestable.APPLE, HarvestableHighlighter.classifyItem("gathering_apple", "minecraft:player_head"));
        assertEquals(Harvestable.MUSHROOM, HarvestableHighlighter.classifyItem("", "minecraft:brown_mushroom_block"));
        assertEquals(Harvestable.COBWEB, HarvestableHighlighter.classifyItem("", "minecraft:cobweb"));
    }

    @Test void modelEngineAndToolsAreNotHarvestables() {
        assertNull(HarvestableHighlighter.classifyItem("", "modelengine:spider_v1/head"));
        assertNull(HarvestableHighlighter.classifyItem("", "modelengine:internal_fire/fire_0"));
        assertNull(HarvestableHighlighter.classifyItem("fishingrod_tidecatcher", "isles:tools/fishing/lurecaster"));
    }

    @Test void blockDisplays() {
        assertEquals(Harvestable.KELP, HarvestableHighlighter.classifyBlock("minecraft:kelp_plant"));
        assertEquals(Harvestable.TANGLEWOOD_VINE, HarvestableHighlighter.classifyBlock("minecraft:cave_vines"));
        assertEquals(Harvestable.DRIFTSTONE, HarvestableHighlighter.classifyBlock("minecraft:cobbled_deepslate"));
        assertEquals(Harvestable.TANGLEWOOD_VINE, HarvestableHighlighter.classifyBlock("minecraft:jungle_sapling"));
        assertNull(HarvestableHighlighter.classifyBlock("minecraft:lime_stained_glass"));
        assertNull(HarvestableHighlighter.classifyBlock("minecraft:deepslate"));
    }
}
