package com.islesplus.features.foodbuff;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The active food buff, read from the tab list, and a seconds countdown for it. No Minecraft
 * types, so it is unit tested.
 * <p>
 * The tab only gives whole minutes ("Active Dish: Ponkberry Smoothie" / "4 mins remaining"), so
 * the seconds come from watching the minutes drop: the moment "4 mins" becomes "3 mins", exactly
 * 3:00 is left, and the clock counts down from there. Until the first drop after joining or
 * eating, the time is an upper-bound estimate, shown with a "~". If the tab is ever seen saying
 * "0 mins" it rounds down rather than up, and every minute count is read as one more.
 */
public final class FoodBuff {
    private static final String DISH = "Active Dish:";
    private static final Pattern MINUTES = Pattern.compile("(\\d+) mins? remaining");

    private FoodBuff() {}

    /** A dish and its minutes left ({@code -1} when the tab did not say). */
    public record Reading(String dish, int minutes) {}

    public static Reading read(List<String> tabRows) {
        for (int i = 0; i < tabRows.size(); i++) {
            String row = tabRows.get(i).strip();
            if (!row.startsWith(DISH)) continue;
            String dish = row.substring(DISH.length()).strip();
            // "Active Dish: NONE" is the tab saying nothing is eaten
            if (dish.isEmpty() || dish.equalsIgnoreCase("NONE")) return null;
            for (int j = i + 1; j < tabRows.size(); j++) {
                String next = tabRows.get(j).strip();
                if (next.isEmpty()) continue;
                Matcher m = MINUTES.matcher(next);
                return new Reading(dish, m.lookingAt() ? Integer.parseInt(m.group(1)) : -1);
            }
            return new Reading(dish, -1);
        }
        return null;
    }

    /** "Ponkberry Smoothie" -> "ponkberry_smoothie", the name its model has in the server pack. */
    public static String modelName(String dish) {
        return dish.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static final class Clock {
        private static final long MINUTE_MS = 60_000L;

        private String dish;
        private int lastMinutes = -1;
        private long endMs;
        private boolean exact;
        /** The tab rounds down ("0 mins" was seen): "3 mins" means 3:00 to 3:59. */
        private boolean roundsDown;

        public void update(String dish, int minutes, long nowMs) {
            if (minutes == 0) roundsDown = true;
            boolean fresh = !dish.equals(this.dish) || lastMinutes < 0 || minutes > lastMinutes;
            this.dish = dish;
            if (minutes < 0) {
                lastMinutes = -1;
                return;
            }
            if (fresh) {
                endMs = nowMs + upTo(minutes);
                exact = false;
            } else if (minutes < lastMinutes) {
                endMs = nowMs + upTo(minutes);
                exact = true;
            }
            lastMinutes = minutes;
        }

        /** How long is left at most when the tab shows {@code minutes} - and exactly that long
         * at the moment the number drops to it. */
        private long upTo(int minutes) {
            return (roundsDown ? minutes + 1 : minutes) * MINUTE_MS;
        }

        public boolean exact() {
            return exact;
        }

        /** "2:47", "3:05:24" from an hour up, or "~3:59" while it is an estimate; empty when the
         * tab gave no time. */
        public String text(long nowMs) {
            if (lastMinutes < 0) return "";
            long seconds = Math.max(0, (endMs - nowMs + 999) / 1000);
            String time = seconds >= 3600
                ? String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                : String.format("%d:%02d", seconds / 60, seconds % 60);
            return (exact ? "" : "~") + time;
        }

        public boolean done(long nowMs) {
            return lastMinutes >= 0 && nowMs >= endMs;
        }
    }
}
