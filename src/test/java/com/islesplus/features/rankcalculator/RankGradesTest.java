package com.islesplus.features.rankcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankGradesTest {
    @Test
    void fullPartyHasNoPlayerDiscount() {
        assertEquals(1.0, RankGrades.multiplier(12, ActiveEvent.NONE), 1e-9);
    }

    @Test
    void soloPlayerDiscountIsUnchanged() {
        assertEquals(0.75 + 1.0 / 12.0, RankGrades.multiplier(1, ActiveEvent.NONE), 1e-9);
    }

    @Test
    void riftSpelunkerTakesTenPercentOffTheMultiplier() {
        assertEquals(0.9, RankGrades.multiplier(12, ActiveEvent.RIFT_SPELUNKER), 1e-9);
        assertEquals((0.75 + 1.0 / 12.0) * 0.9, RankGrades.multiplier(1, ActiveEvent.RIFT_SPELUNKER), 1e-9);
    }

    @Test
    void otherEventsLeaveTheMultiplierAlone() {
        assertEquals(1.0, RankGrades.multiplier(12, ActiveEvent.DUNGEON_DROPPER), 1e-9);
    }

    @Test
    void scoreBetweenScaledAndNormalSThresholdIsAWithoutTheEvent() {
        assertEquals("A", RankGrades.grade(0.85, RankGrades.multiplier(12, ActiveEvent.NONE)));
    }

    @Test
    void sameScoreIsSDuringRiftSpelunker() {
        assertEquals("S", RankGrades.grade(0.85, RankGrades.multiplier(12, ActiveEvent.RIFT_SPELUNKER)));
    }

    @Test
    void scoreBelowEveryThresholdIsF() {
        assertEquals("F", RankGrades.grade(0.10, 1.0));
    }

    @Test
    void thresholdItselfIsNotEnough() {
        assertEquals("A", RankGrades.grade(0.900, 1.0));
    }
}
