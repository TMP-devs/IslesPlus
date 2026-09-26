package com.islesplus.features.plushiefinder;

import com.islesplus.sync.FeatureFlags;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlushieFinder {
    private static final Pattern STRIP_CODES = Pattern.compile("§.");
    /** anchored to the start so someone can't just type it in guild chat and spoof it */
    private static final Pattern FOUND_PATTERN =
        Pattern.compile("(?i)^you\\s+found\\s+plushy\\s*#?(\\d+).*agility\\s+exp");
    public static boolean plushieFinderEnabled = false;
    /** Skip plushie #1 (locked behind the tutorial) when picking the nearest one to point at. It
     * still counts as found or missing everywhere else. */
    public static boolean hideFirstPlushie = false;
    /** only tag the nearest plushie if it's within this many blocks. 0 = no limit */
    public static int maxDistance = 0;
    public static final int MAX_DISTANCE_CAP = 200;
    /** Tightest range the slider can set; below this the feature would never tag anything. */
    private static final int MIN_DISTANCE = 5;
    /** The last few blocks of travel mean "no limit", so MAX is easy to hit at any slider width. */
    private static final int MAX_SNAP = MAX_DISTANCE_CAP - MIN_DISTANCE;

    private PlushieFinder() {}

    /** Slider is 0..1: far left is the tightest range, far right is MAX (no limit). */
    public static float maxDistanceSlider() {
        return maxDistance <= 0 ? 1f : maxDistance / (float) MAX_DISTANCE_CAP;
    }

    public static void setMaxDistanceFromSlider(float v) {
        int blocks = Math.round(v * MAX_DISTANCE_CAP);
        maxDistance = blocks >= MAX_SNAP ? 0 : Math.max(MIN_DISTANCE, blocks);
    }

    public static String maxDistanceLabel() {
        return maxDistance <= 0 ? "Range: Max" : "Range: " + maxDistance + " blocks";
    }

    /** gets every chat message, marks the plushie owned if it's the "you found plushy #N" line */
    public static void onMessage(String text) {
        if (FeatureFlags.isKilled("plushie_finder")) return;
        String clean = STRIP_CODES.matcher(text).replaceAll("").trim();
        Matcher m = FOUND_PATTERN.matcher(clean);
        if (!m.find()) return;
        try {
            int num = Integer.parseInt(m.group(1));
            PlushieRepository.setOwned(num, true);
        } catch (NumberFormatException ignored) {}
    }
}
