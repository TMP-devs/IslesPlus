package com.islesplus.features.eggtimer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EggsTest {
    private static final long GAME_MIN = Eggs.REAL_MS_PER_GAME_MINUTE;

    private static int at(int hour, int minute) {
        return hour * 60 + minute;
    }

    // ---- the Isles clock, off the tab (scan 2026-09-24 05:23) -----------------------------------

    @Test void theClockIsReadOffTheTab() {
        assertEquals(at(9, 26), Eggs.minuteOfDay(List.of(" Rested Exp: 0s", " YEAR 90 09:26 ", " Day: 29 Month: 10")));
        assertEquals(at(23, 17), Eggs.minuteOfDay(List.of(" YEAR 90 23:17 ")));
    }

    @Test void noClockOnTheTabIsNoClock() {
        assertEquals(-1, Eggs.minuteOfDay(List.of(" Day: 29 Month: 10", "Adventurer Scrolls")));
        assertEquals(-1, Eggs.minuteOfDay(List.of(" YEAR 90 25:00")));
    }

    // ---- seconds between the clock's minutes ----------------------------------------------------

    /** The clock shows whole minutes; a minute changing is its exact start. */
    @Test void aMinuteTickingOverIsItsStart() {
        Eggs.Clock clock = new Eggs.Clock();
        clock.update(at(3, 59), 0);
        clock.update(at(4, 0), 1_000);
        assertEquals(at(4, 0) * GAME_MIN, clock.position(1_000));
        assertEquals(at(4, 0) * GAME_MIN + 1_200, clock.position(2_200));
    }

    /** It never runs past the minute the tab still shows. */
    @Test void itWaitsForTheNextMinute() {
        Eggs.Clock clock = new Eggs.Clock();
        clock.update(at(4, 0), 0);
        assertEquals(at(4, 1) * GAME_MIN - 1, clock.position(60_000));
    }

    @Test void noReadingNoPosition() {
        assertEquals(-1, new Eggs.Clock().position(0));
    }

    // ---- what shows when ------------------------------------------------------------------------

    @Test void beforeFourNothing() {
        assertEquals(Eggs.Phase.NONE, Eggs.phase(at(3, 59) * GAME_MIN));
    }

    /** 4 AM on the Isles clock is 5 real minutes before 6 AM. */
    @Test void fromFourTheCountdown() {
        assertEquals(Eggs.Phase.COUNTDOWN, Eggs.phase(at(4, 0) * GAME_MIN));
        assertEquals(5 * 60_000L, Eggs.msUntilSpawn(at(4, 0) * GAME_MIN));
        assertEquals(Eggs.Phase.COUNTDOWN, Eggs.phase(at(6, 0) * GAME_MIN - 1));
    }

    @Test void fromSixToTenSpawning() {
        assertEquals(Eggs.Phase.SPAWNING, Eggs.phase(at(6, 0) * GAME_MIN));
        assertEquals(Eggs.Phase.SPAWNING, Eggs.phase(at(10, 0) * GAME_MIN - 1));
        assertEquals(Eggs.Phase.NONE, Eggs.phase(at(10, 0) * GAME_MIN));
    }

    @Test void countdownReads() {
        assertEquals("5:00", Eggs.countdown(5 * 60_000L));
        assertEquals("0:01", Eggs.countdown(1));
        assertEquals("2:30", Eggs.countdown(150_000L));
    }

    // ---- the warning ----------------------------------------------------------------------------

    @Test void theWarningComesOnceAtFour() {
        Eggs.Warning warning = new Eggs.Warning();
        assertFalse(warning.update(at(3, 59) * GAME_MIN));
        assertTrue(warning.update(at(4, 0) * GAME_MIN));
        assertFalse(warning.update(at(4, 1) * GAME_MIN));
        assertFalse(warning.update(at(6, 30) * GAME_MIN));
    }

    /** Joining at 5 AM: the countdown is up, but a warning 2.5 real minutes late is not shown. */
    @Test void aLateWarningIsNotShown() {
        Eggs.Warning warning = new Eggs.Warning();
        assertFalse(warning.update(at(5, 0) * GAME_MIN));
    }

    @Test void aLittleLateStillCounts() {
        Eggs.Warning warning = new Eggs.Warning();
        assertTrue(warning.update(at(4, 5) * GAME_MIN));
    }

    // ---- the server saying the eggs are out (screenshot 2026-09-24 22:16) ------------------------

    @Test void theNestsMessageIsKnown() {
        assertTrue(Eggs.isNestsMessage("PET NESTS! New eggs have been laid in nests around the world. Go collect those Pets!"));
        assertFalse(Eggs.isNestsMessage("lifeontop » yay pets"));
        assertFalse(Eggs.isNestsMessage("Code_Rekt1 » PET NESTS! soon?"));
        assertFalse(Eggs.isNestsMessage("Code_Rekt1 » PET NESTS! New eggs have been laid lol"));
        assertTrue(Eggs.isNestsMessage(" PET NESTS! New eggs have been laid in nests around the world."));
    }

    @Test void withoutTheMessageThePhaseShows() {
        Eggs.Nests nests = new Eggs.Nests();
        assertEquals(Eggs.View.COUNTDOWN, nests.view(Eggs.Phase.COUNTDOWN, 0));
        assertEquals(Eggs.View.SPAWNING, nests.view(Eggs.Phase.SPAWNING, 0));
        assertEquals(Eggs.View.NONE, nests.view(Eggs.Phase.NONE, 0));
    }

    @Test void theMessageShowsSpawnedForThirtySecondsThenHides() {
        Eggs.Nests nests = new Eggs.Nests();
        nests.heard(1_000);
        assertEquals(Eggs.View.SPAWNED, nests.view(Eggs.Phase.SPAWNING, 1_000));
        assertEquals(Eggs.View.SPAWNED, nests.view(Eggs.Phase.SPAWNING, 30_999));
        assertEquals(Eggs.View.NONE, nests.view(Eggs.Phase.SPAWNING, 31_000));
    }

    /** Hidden until it is time to warn again: the next Isles day counts down as usual. */
    @Test void theNextDayShowsAgain() {
        Eggs.Nests nests = new Eggs.Nests();
        nests.heard(0);
        assertEquals(Eggs.View.NONE, nests.view(Eggs.Phase.SPAWNING, 60_000));
        assertEquals(Eggs.View.NONE, nests.view(Eggs.Phase.NONE, 600_000));
        assertEquals(Eggs.View.COUNTDOWN, nests.view(Eggs.Phase.COUNTDOWN, 3_600_000));
    }

    /** Eggs out before 6 AM: spawned, then hidden - not the rest of the countdown. */
    @Test void anEarlySpawnEndsTheCountdown() {
        Eggs.Nests nests = new Eggs.Nests();
        nests.heard(0);
        assertEquals(Eggs.View.SPAWNED, nests.view(Eggs.Phase.COUNTDOWN, 10_000));
        assertEquals(Eggs.View.NONE, nests.view(Eggs.Phase.COUNTDOWN, 40_000));
        assertEquals(Eggs.View.NONE, nests.view(Eggs.Phase.SPAWNING, 100_000));
    }

    /** The next Isles day (the next real hour) warns again. */
    @Test void theNextDayWarnsAgain() {
        Eggs.Warning warning = new Eggs.Warning();
        assertTrue(warning.update(at(4, 0) * GAME_MIN));
        assertFalse(warning.update(at(12, 0) * GAME_MIN));
        assertFalse(warning.update(at(3, 0) * GAME_MIN));
        assertTrue(warning.update(at(4, 0) * GAME_MIN));
    }
}
