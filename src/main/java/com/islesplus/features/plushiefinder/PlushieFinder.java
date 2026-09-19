package com.islesplus.features.plushiefinder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlushieFinder {
    private static final Pattern STRIP_CODES = Pattern.compile("§.");
    /** anchored to the start so someone can't just type it in guild chat and spoof it */
    private static final Pattern FOUND_PATTERN =
        Pattern.compile("(?i)^you\\s+found\\s+plushy\\s*#?(\\d+).*agility\\s+exp");
    public static boolean plushieFinderEnabled = false;
    /** only tag the nearest plushie if it's within this many blocks. 0 = no limit */
    public static int maxDistance = 0;
    public static final int MAX_DISTANCE_CAP = 200;

    private PlushieFinder() {}

    // slider is 0..1, anything under 5 blocks counts as off
    public static float maxDistanceSlider() {
        return maxDistance / (float) MAX_DISTANCE_CAP;
    }

    public static void setMaxDistanceFromSlider(float v) {
        int blocks = Math.round(v * MAX_DISTANCE_CAP);
        maxDistance = blocks < 5 ? 0 : blocks;
    }

    public static String maxDistanceLabel() {
        return maxDistance <= 0 ? "Range: Max" : "Range: " + maxDistance + " blocks";
    }

    /** gets every chat message, marks the plushie owned if it's the "you found plushy #N" line */
    public static void onMessage(String text) {
        String clean = STRIP_CODES.matcher(text).replaceAll("").trim();
        Matcher m = FOUND_PATTERN.matcher(clean);
        if (!m.find()) return;
        try {
            int num = Integer.parseInt(m.group(1));
            PlushieRepository.setOwned(num, true);
        } catch (NumberFormatException ignored) {}
    }
}
