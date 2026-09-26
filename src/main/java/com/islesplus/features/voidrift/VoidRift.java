package com.islesplus.features.voidrift;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * When the Void Rift spawns, and how that reads. No Minecraft types, so it is unit tested.
 * <p>
 * It spawns every 7 hours, counted in UTC from one known spawn: 25 Sep 2026 05:00 UTC. Nobody's
 * clocks move it; only {@link #localTime} turns it into the player's own time zone.
 */
public final class VoidRift {
    static final long ANCHOR_MS = Instant.parse("2026-09-25T05:00:00Z").toEpochMilli();
    static final long PERIOD_MS = 7 * 60 * 60_000L;
    private static final long HOUR_MS = 60 * 60_000L;

    private VoidRift() {}

    /** The first spawn after {@code nowMs} (at the moment of one, the one after it). */
    public static long nextSpawn(long nowMs) {
        return nowMs - Math.floorMod(nowMs - ANCHOR_MS, PERIOD_MS) + PERIOD_MS;
    }

    /** Alert mode's corner countdown is up for the last hour before a spawn. */
    public static boolean inLastHour(long msLeft) {
        return msLeft <= HOUR_MS;
    }

    /** The on-screen alerts, in the order they come: a "Void Rift" title, "in 1 hr" under it. */
    public enum Alert {
        HOUR(60, "in 1 hr"),
        TEN_MINUTES(10, "in 10 min"),
        FIVE_MINUTES(5, "in 5 min");

        final long atMsLeft;
        public final String subtitle;
        public final String text;

        Alert(int minutes, String subtitle) {
            this.atMsLeft = minutes * 60_000L;
            this.subtitle = subtitle;
            this.text = "Void Rift " + subtitle;
        }
    }

    /** The alert title's timing, like a vanilla title: it fades in, holds, and fades out. */
    static final long FADE_IN_MS = 500, FADE_OUT_MS = 1_500, TITLE_MS = 10_000;

    /** How opaque the alert title is {@code ms} after it came up (0 to 1), or -1 once it is over. */
    public static float titleAlpha(long ms) {
        if (ms < 0 || ms >= TITLE_MS) return -1f;
        if (ms < FADE_IN_MS) return ms / (float) FADE_IN_MS;
        long left = TITLE_MS - ms;
        return left < FADE_OUT_MS ? left / (float) FADE_OUT_MS : 1f;
    }

    /** Which alert is due, each once per spawn. */
    public static final class Alerts {
        /** An alert missed by up to this much (lag, a loading screen) still shows; one further
         * back does not - joining with 40 minutes left does not bring up "in 1 hr". */
        private static final long LATE_MS = 30_000L;

        private long spawn = Long.MIN_VALUE;
        private int fired;

        /** The alert to show now, or null. */
        public Alert update(long spawnMs, long nowMs) {
            if (spawnMs != spawn) {
                spawn = spawnMs;
                fired = 0;
            }
            long left = spawnMs - nowMs;
            for (Alert a : Alert.values()) {
                int bit = 1 << a.ordinal();
                if ((fired & bit) != 0 || left > a.atMsLeft) continue;
                fired |= bit;
                if (left > a.atMsLeft - LATE_MS) return a;
            }
            return null;
        }
    }

    /** "5:42:17", or "42:17" under an hour; rounded up, so it reads 0:00 only at the spawn. */
    public static String countdown(long msLeft) {
        long s = Math.max(0, (msLeft + 999) / 1000);
        return s >= 3600
            ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
            : String.format("%d:%02d", s / 60, s % 60);
    }

    /** AP style: the short months in full, the rest cut with a point. */
    private static final String[] MONTHS = {"Jan.", "Feb.", "March", "April", "May", "June", "July",
        "Aug.", "Sept.", "Oct.", "Nov.", "Dec."};

    /** "Sept. 25 12:00 AM": the spawn at {@code spawnMs} in {@code zone}. */
    public static String localTime(long spawnMs, ZoneId zone) {
        ZonedDateTime t = Instant.ofEpochMilli(spawnMs).atZone(zone);
        int hour12 = t.getHour() % 12 == 0 ? 12 : t.getHour() % 12;
        return String.format("%s %d %d:%02d %s", MONTHS[t.getMonthValue() - 1], t.getDayOfMonth(),
            hour12, t.getMinute(), t.getHour() < 12 ? "AM" : "PM");
    }
}
