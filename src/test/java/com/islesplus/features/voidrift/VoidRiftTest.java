package com.islesplus.features.voidrift;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class VoidRiftTest {
    private static final long MIN = 60_000L, HOUR = 60 * MIN;
    /** A known spawn, 25 Sep 2026 05:00 UTC. */
    private static final long ANCHOR = Instant.parse("2026-09-25T05:00:00Z").toEpochMilli();

    // ---- when -----------------------------------------------------------------------------------

    @Test void theAnchorIsAKnownSpawnInUtc() {
        assertEquals(ANCHOR, VoidRift.ANCHOR_MS);
    }

    @Test void theNextSpawnIsTheAnchorBeforeIt() {
        assertEquals(ANCHOR, VoidRift.nextSpawn(ANCHOR - 3 * HOUR));
    }

    @Test void spawnsComeEverySevenHoursAfterIt() {
        assertEquals(ANCHOR + 7 * HOUR, VoidRift.nextSpawn(ANCHOR + 1));
        assertEquals(ANCHOR + 70 * HOUR, VoidRift.nextSpawn(ANCHOR + 65 * HOUR));
    }

    /** At the very moment of a spawn it has happened: the next one is 7 hours on. */
    @Test void atTheSpawnItselfTheNextOneIsNext() {
        assertEquals(ANCHOR + 7 * HOUR, VoidRift.nextSpawn(ANCHOR));
    }

    @Test void andEverySevenHoursBeforeIt() {
        assertEquals(ANCHOR - 7 * HOUR, VoidRift.nextSpawn(ANCHOR - 8 * HOUR));
    }

    // ---- alerts ---------------------------------------------------------------------------------

    @Test void eachAlertFiresOnceAsItsTimeComes() {
        VoidRift.Alerts alerts = new VoidRift.Alerts();
        long spawn = ANCHOR;
        assertNull(alerts.update(spawn, spawn - 61 * MIN));
        assertEquals(VoidRift.Alert.HOUR, alerts.update(spawn, spawn - 60 * MIN));
        assertNull(alerts.update(spawn, spawn - 59 * MIN));
        assertEquals(VoidRift.Alert.TEN_MINUTES, alerts.update(spawn, spawn - 10 * MIN + 500));
        assertNull(alerts.update(spawn, spawn - 9 * MIN));
        assertEquals(VoidRift.Alert.FIVE_MINUTES, alerts.update(spawn, spawn - 5 * MIN));
        assertNull(alerts.update(spawn, spawn - 1));
    }

    /** Joining with 40 minutes left: the hour alert is long past and is not shown late. */
    @Test void aLongPastAlertIsNotShownLate() {
        VoidRift.Alerts alerts = new VoidRift.Alerts();
        assertNull(alerts.update(ANCHOR, ANCHOR - 40 * MIN));
        assertEquals(VoidRift.Alert.TEN_MINUTES, alerts.update(ANCHOR, ANCHOR - 10 * MIN));
    }

    /** A moment of lag across the threshold still counts. */
    @Test void anAlertJustMissedStillFires() {
        VoidRift.Alerts alerts = new VoidRift.Alerts();
        assertEquals(VoidRift.Alert.HOUR, alerts.update(ANCHOR, ANCHOR - 60 * MIN + 20_000));
    }

    @Test void theNextSpawnGetsItsOwnAlerts() {
        VoidRift.Alerts alerts = new VoidRift.Alerts();
        assertEquals(VoidRift.Alert.FIVE_MINUTES, alerts.update(ANCHOR, ANCHOR - 5 * MIN));
        long next = ANCHOR + 7 * HOUR;
        assertEquals(VoidRift.Alert.HOUR, alerts.update(next, next - 60 * MIN));
    }

    // ---- the alert as a title: fade in, hold, fade out -------------------------------------------

    @Test void theTitleFadesIn() {
        assertEquals(0f, VoidRift.titleAlpha(0), 1e-6);
        assertEquals(0.5f, VoidRift.titleAlpha(250), 1e-6);
        assertEquals(1f, VoidRift.titleAlpha(500), 1e-6);
    }

    @Test void holdsThenFadesOut() {
        assertEquals(1f, VoidRift.titleAlpha(8_500), 1e-6);
        assertEquals(0.5f, VoidRift.titleAlpha(9_250), 1e-6);
    }

    /** Ten seconds in all, then gone. */
    @Test void thenItIsGone() {
        assertTrue(VoidRift.titleAlpha(9_999) > 0);
        assertEquals(-1f, VoidRift.titleAlpha(10_000));
    }

    @Test void theSubtitleIsWhenItComes() {
        assertEquals("in 1 hr", VoidRift.Alert.HOUR.subtitle);
        assertEquals("in 10 min", VoidRift.Alert.TEN_MINUTES.subtitle);
        assertEquals("in 5 min", VoidRift.Alert.FIVE_MINUTES.subtitle);
    }

    @Test void alertTexts() {
        assertEquals("Void Rift in 1 hr", VoidRift.Alert.HOUR.text);
        assertEquals("Void Rift in 10 min", VoidRift.Alert.TEN_MINUTES.text);
        assertEquals("Void Rift in 5 min", VoidRift.Alert.FIVE_MINUTES.text);
    }

    /** Alert mode's corner countdown is up for the last hour only. */
    @Test void theLastHourCountdownShowsWithinTheHour() {
        assertFalse(VoidRift.inLastHour(60 * MIN + 1));
        assertTrue(VoidRift.inLastHour(60 * MIN));
        assertTrue(VoidRift.inLastHour(1));
    }

    // ---- how it reads ---------------------------------------------------------------------------

    @Test void countdownReadsInHoursMinutesSeconds() {
        assertEquals("6:59:59", VoidRift.countdown(7 * HOUR - 1000));
        assertEquals("1:00:00", VoidRift.countdown(HOUR));
        assertEquals("42:17", VoidRift.countdown(42 * MIN + 17_000));
        assertEquals("0:05", VoidRift.countdown(5_000));
    }

    /** Rounded up, so it never reads 0:00 before the spawn. */
    @Test void countdownRoundsUpToTheSecond() {
        assertEquals("0:01", VoidRift.countdown(1));
        assertEquals("0:00", VoidRift.countdown(0));
    }

    @Test void localTimeIsTheSpawnInThePlayersZone() {
        assertEquals("Sept. 25 12:00 AM", VoidRift.localTime(ANCHOR, ZoneId.of("America/Chicago")));
        assertEquals("Sept. 25 5:00 AM", VoidRift.localTime(ANCHOR, ZoneId.of("UTC")));
        assertEquals("Sept. 24 10:00 PM", VoidRift.localTime(ANCHOR, ZoneId.of("America/Los_Angeles")));
    }

    /** AP style months: short ones in full, the rest abbreviated with a point. */
    @Test void monthsReadAsWritten() {
        ZoneId utc = ZoneId.of("UTC");
        assertEquals("May 3 7:05 PM", VoidRift.localTime(Instant.parse("2027-05-03T19:05:00Z").toEpochMilli(), utc));
        assertEquals("Oct. 1 12:30 PM", VoidRift.localTime(Instant.parse("2026-10-01T12:30:00Z").toEpochMilli(), utc));
    }

    /** Across the clocks going back (1 Nov 2026 in the US), spawns stay 7 real hours apart. */
    @Test void daylightSavingDoesNotMoveTheSchedule() {
        long spawn = VoidRift.nextSpawn(Instant.parse("2026-11-01T12:00:00Z").toEpochMilli());
        assertEquals(0, (spawn - ANCHOR) % (7 * HOUR));
    }
}
