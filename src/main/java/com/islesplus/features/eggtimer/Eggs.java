package com.islesplus.features.eggtimer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When eggs spawn, from the Isles clock. No Minecraft types, so it is unit tested.
 * <p>
 * Eggs spawn between 6 and 10 AM on the Isles clock - the "YEAR 90 09:26" row of the tab list. An
 * Isles day is one real hour (scans 2026-09-24: 2.5 real seconds a clock minute, and the clock
 * reads the minutes past the real hour times 24), so 6 AM is 5 real minutes after 4 AM, and the
 * spawn window is 10 real minutes long.
 * <p>
 * Everything here is in real milliseconds into the Isles day: {@code minute * }
 * {@link #REAL_MS_PER_GAME_MINUTE}.
 */
public final class Eggs {
    static final long REAL_MS_PER_GAME_MINUTE = 2_500L;
    private static final Pattern CLOCK = Pattern.compile("YEAR \\d+ (\\d{1,2}):(\\d{2})");

    static final long WARN_AT = 4 * 60 * REAL_MS_PER_GAME_MINUTE;
    static final long SPAWN_FROM = 6 * 60 * REAL_MS_PER_GAME_MINUTE;
    static final long SPAWN_UNTIL = 10 * 60 * REAL_MS_PER_GAME_MINUTE;

    private Eggs() {}

    /** The Isles clock's minute of the day off the tab, or -1 when it is not there. */
    public static int minuteOfDay(List<String> tabRows) {
        for (String row : tabRows) {
            Matcher m = CLOCK.matcher(row);
            if (!m.find()) continue;
            int hour = Integer.parseInt(m.group(1)), minute = Integer.parseInt(m.group(2));
            if (hour < 24 && minute < 60) return hour * 60 + minute;
        }
        return -1;
    }

    /** The clock to the millisecond: the tab gives whole minutes, and a minute ticking over is
     * the exact start of the new one. */
    public static final class Clock {
        private int minute = -1;
        private long minuteStartMs;

        public void update(int minuteOfDay, long nowMs) {
            if (minuteOfDay != minute) {
                minute = minuteOfDay;
                minuteStartMs = nowMs;
            }
        }

        /** Real ms into the Isles day, or -1 with no reading yet. Never past the minute shown. */
        public long position(long nowMs) {
            if (minute < 0) return -1;
            long into = Math.min(Math.max(0, nowMs - minuteStartMs), REAL_MS_PER_GAME_MINUTE - 1);
            return minute * REAL_MS_PER_GAME_MINUTE + into;
        }
    }

    public enum Phase { NONE, COUNTDOWN, SPAWNING }

    public static Phase phase(long position) {
        if (position >= WARN_AT && position < SPAWN_FROM) return Phase.COUNTDOWN;
        if (position >= SPAWN_FROM && position < SPAWN_UNTIL) return Phase.SPAWNING;
        return Phase.NONE;
    }

    public static long msUntilSpawn(long position) {
        return SPAWN_FROM - position;
    }

    /** "5:00", rounded up so it reads 0:00 only at 6 AM. */
    public static String countdown(long msLeft) {
        long s = Math.max(0, (msLeft + 999) / 1000);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    /** The server's own word that the eggs are out: "PET NESTS! New eggs have been laid in nests
     * around the world. Go collect those Pets!" - its line, not a player quoting it (player chat
     * reads "name » text"). Matched inside the line, in case the server puts an icon glyph first. */
    public static boolean isNestsMessage(String text) {
        return text.contains("PET NESTS!") && text.contains("New eggs have been laid") && !text.contains("»");
    }

    /** What the corner shows. */
    public enum View { NONE, COUNTDOWN, SPAWNING, SPAWNED }

    /** After the nests message: "Eggs spawned" for 30 s, then nothing until the next Isles day
     * counts down again. The quiet is timed rather than tied to the window closing, so it ends even
     * if nobody was on an Isle to see the window close: the window is over at most 15 minutes after
     * the message (5 early, 10 long), and the next warning is 45 or more away. */
    public static final class Nests {
        static final long SPAWNED_SHOWN_MS = 30_000L;
        static final long QUIET_MS = 30 * 60_000L;

        private long heardAtMs = -1;

        public void heard(long nowMs) {
            heardAtMs = nowMs;
        }

        public View view(Phase phase, long nowMs) {
            long since = heardAtMs < 0 ? Long.MAX_VALUE : nowMs - heardAtMs;
            if (since < SPAWNED_SHOWN_MS) return View.SPAWNED;
            if (since < QUIET_MS || phase == Phase.NONE) return View.NONE;
            return phase == Phase.COUNTDOWN ? View.COUNTDOWN : View.SPAWNING;
        }
    }

    /** The 4 AM warning, once an Isles day. */
    public static final class Warning {
        /** A warning missed by up to this much (lag, a loading screen) still shows; joining well
         * into the countdown does not bring it up late. */
        private static final long LATE_MS = 30_000L;

        private boolean warned;

        /** Whether to warn now. */
        public boolean update(long position) {
            if (phase(position) != Phase.COUNTDOWN) {
                if (phase(position) == Phase.NONE) warned = false;
                return false;
            }
            if (warned) return false;
            warned = true;
            return position - WARN_AT <= LATE_MS;
        }
    }
}
