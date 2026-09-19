package com.islesplus.features.rankcalculator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveEventTest {
    @Test
    void riftSpelunkerIsDetectedInTabText() {
        assertEquals(ActiveEvent.RIFT_SPELUNKER,
            ActiveEvent.detect(List.of("Skyblock Isles", "CURRENT EVENT: Rift Spelunker")));
    }

    @Test
    void formattingCodesAreIgnored() {
        assertEquals(ActiveEvent.RIFT_SPELUNKER,
            ActiveEvent.detect(List.of("§6§lCURRENT EVENT: §eRift §eSpelunker")));
    }

    @Test
    void matchIsCaseInsensitive() {
        assertEquals(ActiveEvent.RIFT_SPELUNKER, ActiveEvent.detect(List.of("RIFT SPELUNKER")));
    }

    @Test
    void noEventTextMeansNone() {
        assertEquals(ActiveEvent.NONE, ActiveEvent.detect(List.of("Skyblock Isles", "Players: 42", "Steve")));
    }

    @Test
    void emptyTabMeansNone() {
        assertEquals(ActiveEvent.NONE, ActiveEvent.detect(List.of()));
    }

    @Test
    void everyOtherEventIsRecognised() {
        assertEquals(ActiveEvent.FARMING_FRENZY, ActiveEvent.detect(List.of("Farming Frenzy")));
        assertEquals(ActiveEvent.WOODCUTTING_FRENZY, ActiveEvent.detect(List.of("Woodcutting Frenzy")));
        assertEquals(ActiveEvent.MINING_FRENZY, ActiveEvent.detect(List.of("Mining Frenzy")));
        assertEquals(ActiveEvent.FISHING_FRENZY, ActiveEvent.detect(List.of("Fishing Frenzy")));
        assertEquals(ActiveEvent.BOSS_BIAS, ActiveEvent.detect(List.of("Boss Bias")));
        assertEquals(ActiveEvent.DUNGEON_DROPPER, ActiveEvent.detect(List.of("Dungeon Dropper")));
        assertEquals(ActiveEvent.MINION_FRENZY, ActiveEvent.detect(List.of("Minion Frenzy")));
        assertEquals(ActiveEvent.PET_SHAREHOLDER, ActiveEvent.detect(List.of("Pet Shareholder")));
    }

    @Test
    void onlyRiftSpelunkerLowersGradeThresholds() {
        for (ActiveEvent event : ActiveEvent.values()) {
            assertEquals(event == ActiveEvent.RIFT_SPELUNKER ? 0.9 : 1.0, event.thresholdScale(), 1e-9, event.name());
        }
    }
}
