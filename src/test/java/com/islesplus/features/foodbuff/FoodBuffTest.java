package com.islesplus.features.foodbuff;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodBuffTest {

    /** The tab rows around the dish, as /ip scan read them on 2026-09-24. */
    private static final List<String> TAB = List.of(
        "  Walk Speed: 136%", "Bartender", " ", " Active Dish: Ponkberry Smoothie", " 4 mins remaining", " ", "Pantoffy");

    // ---- reading the tab ---------------------------------------------------------------------

    @Test void theDishAndItsMinutesAreReadFromTheTab() {
        FoodBuff.Reading r = FoodBuff.read(TAB);
        assertEquals("Ponkberry Smoothie", r.dish());
        assertEquals(4, r.minutes());
    }

    @Test void oneMinuteIsSingular() {
        assertEquals(1, FoodBuff.read(List.of("Active Dish: Oat Bar", "1 min remaining")).minutes());
    }

    @Test void noDishNoReading() {
        assertNull(FoodBuff.read(List.of("  Walk Speed: 136%", "Pantoffy")));
    }

    /** With nothing eaten the tab says "Active Dish: NONE" (scan 2026-09-24 04:58): no dish. */
    @Test void noneIsNoDish() {
        assertNull(FoodBuff.read(List.of(" Active Dish: NONE", "")));
        assertNull(FoodBuff.read(List.of(" Active Dish: None")));
    }

    @Test void aDishWithoutAMinutesRowHasNoTime() {
        FoodBuff.Reading r = FoodBuff.read(List.of("Active Dish: Oat Bar", "Pantoffy"));
        assertEquals("Oat Bar", r.dish());
        assertEquals(-1, r.minutes());
    }

    // ---- the icon's model --------------------------------------------------------------------

    @Test void theModelNameIsTheDishInSnakeCase() {
        assertEquals("ponkberry_smoothie", FoodBuff.modelName("Ponkberry Smoothie"));
        assertEquals("grilled_flounder", FoodBuff.modelName("Grilled Flounder"));
        assertEquals("oat_bar", FoodBuff.modelName("Oat Bar"));
    }

    // ---- the clock ---------------------------------------------------------------------------

    @Test void beforeTheFirstMinuteTickTheTimeIsAnEstimate() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Ponkberry Smoothie", 4, 0);
        assertFalse(clock.exact());
        assertEquals("~4:00", clock.text(0));
        assertEquals("~3:59", clock.text(1_000));
    }

    /** "4 mins" drops to "3 mins" the moment 3:00 is left, so that moment is exact. */
    @Test void aMinuteTickMakesTheTimeExact() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Ponkberry Smoothie", 4, 0);
        clock.update("Ponkberry Smoothie", 3, 20_000);
        assertTrue(clock.exact());
        assertEquals("3:00", clock.text(20_000));
        assertEquals("2:47", clock.text(33_000));
    }

    @Test void theSameMinuteAgainChangesNothing() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Ponkberry Smoothie", 4, 0);
        clock.update("Ponkberry Smoothie", 3, 20_000);
        clock.update("Ponkberry Smoothie", 3, 40_000);
        assertEquals("2:40", clock.text(40_000));
    }

    @Test void eatingAgainStartsOver() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Ponkberry Smoothie", 4, 0);
        clock.update("Ponkberry Smoothie", 3, 20_000);
        clock.update("Ponkberry Smoothie", 10, 30_000);   // minutes went up: a fresh one
        assertFalse(clock.exact());
        assertEquals("~10:00", clock.text(30_000));
    }

    @Test void aDifferentDishStartsOver() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Ponkberry Smoothie", 4, 0);
        clock.update("Ponkberry Smoothie", 3, 20_000);
        clock.update("Oat Bar", 3, 25_000);
        assertFalse(clock.exact());
    }

    /** If the tab ever says "0 mins", it rounds down, not up: each tick then means a whole
     * minute more than the number it shows. */
    @Test void seeingZeroMinutesMeansTheTabRoundsDown() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Oat Bar", 0, 0);
        clock.update("Oat Bar", 1, 1_000);   // re-ate: 1:00 to 1:59 left
        clock.update("Oat Bar", 0, 50_000);  // the drop happens when 1:00 is left
        assertEquals("1:00", clock.text(50_000));
    }

    /** 2026-09-24 in game: a smoothie with over three hours left read "185:24". */
    @Test void anHourOrMoreShowsHours() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Ponkberry Smoothie", 186, 0);
        clock.update("Ponkberry Smoothie", 185, 0);
        assertEquals("3:05:00", clock.text(0));
        assertEquals("3:04:36", clock.text(24_000));
        assertEquals("59:59", clock.text(185 * 60_000 - 3_599_000));
    }

    @Test void theClockStopsAtZero() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Oat Bar", 1, 0);
        assertTrue(clock.done(60_000));
        assertFalse(clock.done(59_000));
    }

    @Test void noTimeKnownShowsNoTime() {
        FoodBuff.Clock clock = new FoodBuff.Clock();
        clock.update("Oat Bar", -1, 0);
        assertEquals("", clock.text(0));
        assertFalse(clock.done(10_000_000));
    }
}
