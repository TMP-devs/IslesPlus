package com.islesplus.features.rankcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Real run reported by a tester (Goblin Grottos, solo, 2026-09-18): scoreboard 169/206 with
 * 4m39s left of a 618 s rift while the Rift Spelunker event was active. The HUD said "A, +1 pts
 * for S"; the server awarded S, because the event lowers every grade threshold by 10%.
 */
class RiftSpelunkerRegressionTest {
    private static final double RAW_SCORE = ((169.0 / 206.0) * 4.0 + (279.0 / 618.0)) / 5.0;

    @Test void withoutTheEventThisRunIsAnA() {
        assertEquals("A", RankGrades.grade(RAW_SCORE, RankGrades.multiplier(1, ActiveEvent.NONE)));
    }

    @Test void duringRiftSpelunkerTheSameRunIsAnS() {
        assertEquals("S", RankGrades.grade(RAW_SCORE, RankGrades.multiplier(1, ActiveEvent.RIFT_SPELUNKER)));
    }

    @Test void eventScalesTheRequirementByTenPercent() {
        // "if you needed 100 to get S you would only need 90"
        assertEquals(0.9 * RankGrades.multiplier(3, ActiveEvent.NONE),
            RankGrades.multiplier(3, ActiveEvent.RIFT_SPELUNKER), 1e-9);
    }
}
