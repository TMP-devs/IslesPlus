package com.islesplus.features.foodbuff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodBuffLayoutTest {

    /** Scoreboard 100 wide at x=540 on a 640 screen: the line spans the same width, the icon at
     * its left, the time ending short of the screen edge. */
    @Test void aLineThatFitsSpansTheScoreboard() {
        FoodBuffTimer.Layout l = FoodBuffTimer.layout(540, 100, 40, 20);
        assertEquals(540 + FoodBuffTimer.INSET_LEFT, l.iconX());
        assertEquals(640 - FoodBuffTimer.INSET_RIGHT - 20, l.timeX());
    }

    @Test void theNameFollowsTheIcon() {
        FoodBuffTimer.Layout l = FoodBuffTimer.layout(540, 100, 40, 20);
        assertEquals(l.iconX() + FoodBuffTimer.ICON + FoodBuffTimer.GAP, l.nameX());
    }

    // ---- fixed-width time: digits do not dance -----------------------------------------------

    /** Proportional widths: "1" is narrower than the other digits. */
    private static int width(char c) {
        return c == '1' ? 3 : c == ':' ? 2 : c == '~' ? 6 : 6;
    }

    @Test void everyTimeOfTheSameShapeIsTheSameWidth() {
        assertEquals(FoodBuffTimer.timeWidth("2:47", FoodBuffLayoutTest::width),
            FoodBuffTimer.timeWidth("1:11", FoodBuffLayoutTest::width));
    }

    /** The "~" has its own slot whether it shows or not, so dropping it moves nothing. */
    @Test void theEstimateMarkDoesNotChangeTheWidth() {
        assertEquals(FoodBuffTimer.timeWidth("~3:59", FoodBuffLayoutTest::width),
            FoodBuffTimer.timeWidth("3:59", FoodBuffLayoutTest::width));
    }

    @Test void eachCharacterKeepsItsPlaceAsTheSecondsTick() {
        int[] a = FoodBuffTimer.cellXs("2:47", 100, FoodBuffLayoutTest::width);
        int[] b = FoodBuffTimer.cellXs("2:41", 100, FoodBuffLayoutTest::width);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) assertEquals(a[i], b[i]);
    }

    @Test void theLastCellEndsAtTheRightEdge() {
        int[] xs = FoodBuffTimer.cellXs("2:47", 100, FoodBuffLayoutTest::width);
        assertEquals(100 - 6, xs[xs.length - 1]);   // the widest digit is 6
    }

    /** A long dish name is never cut: the line grows to the left instead. */
    @Test void aLineTooLongForTheScoreboardGrowsLeft() {
        FoodBuffTimer.Layout l = FoodBuffTimer.layout(540, 100, 120, 20);
        int right = 640 - FoodBuffTimer.INSET_RIGHT;
        assertEquals(right - 20, l.timeX());
        assertEquals(l.timeX() - FoodBuffTimer.GAP - 120, l.nameX());
        assertEquals(l.nameX() - FoodBuffTimer.GAP - FoodBuffTimer.ICON, l.iconX());
    }
}
