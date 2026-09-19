package com.islesplus.features.harvesttimer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HarvestTimerColorTest {
    @Test void moreThanHalfLeftIsLime() {
        assertEquals(HarvestTimer.LIME, HarvestTimer.colorFor(120, 120));
        assertEquals(HarvestTimer.LIME, HarvestTimer.colorFor(61, 120));
    }

    @Test void halfDownToAQuarterIsGold() {
        assertEquals(HarvestTimer.GOLD, HarvestTimer.colorFor(60, 120));
        assertEquals(HarvestTimer.GOLD, HarvestTimer.colorFor(31, 120));
    }

    @Test void lastQuarterIsRed() {
        assertEquals(HarvestTimer.RED, HarvestTimer.colorFor(30, 120));
        assertEquals(HarvestTimer.RED, HarvestTimer.colorFor(0, 120));
    }

    @Test void moreTimeThanTheFullEstimateStaysLime() {
        assertEquals(HarvestTimer.LIME, HarvestTimer.colorFor(200, 120));
    }

    @Test void unknownFullTimeFallsBackToRed() {
        assertEquals(HarvestTimer.RED, HarvestTimer.colorFor(50, 0));
    }
}
