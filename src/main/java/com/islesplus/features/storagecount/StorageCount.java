package com.islesplus.features.storagecount;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The real amount in a storage slot. Resource Storage (and the quiver) show a stack of at most 99,
 * and put the true amount in the tooltip - "Stored: 580/2048" (scan 2026-09-24 21:25). The slot's
 * number is swapped for that amount. No Minecraft types, so it is unit tested.
 */
public final class StorageCount {
    public static boolean storageCountEnabled = true;

    private static final Pattern STORED = Pattern.compile("^Stored: ([\\d,]+)");

    private StorageCount() {}

    /** The amount on the tooltip's "Stored:" line, or -1 when it has none. */
    public static int stored(List<String> loreLines) {
        for (String line : loreLines) {
            Matcher m = STORED.matcher(line.strip());
            if (!m.find()) continue;
            try {
                return Integer.parseInt(m.group(1).replace(",", ""));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /** "580" exact under a thousand; then "1.2k", "12k", "1.5m" - rounded down, so it never says
     * more than there is, and short enough for a 16 px slot. */
    public static String label(int amount) {
        if (amount < 1_000) return Integer.toString(amount);
        if (amount < 1_000_000) return shorten(amount, 1_000, "k");
        return shorten(amount, 1_000_000, "m");
    }

    /**
     * How big to draw a label {@code width} GUI px wide (at full size) so it fits {@code room}: full
     * size if it fits, else the largest step down that keeps the game's pixel font crisp - one
     * font pixel to a whole number of screen pixels, so steps of 1/guiScale - and never below one
     * screen pixel per font pixel.
     */
    public static float fitScale(int width, int guiScale, int room) {
        int gui = Math.max(1, guiScale);
        for (int k = gui; k > 1; k--) {
            if (width * k <= room * gui) return k / (float) gui;
        }
        return 1f / gui;
    }

    private static String shorten(int amount, int unit, String suffix) {
        int whole = amount / unit;
        if (whole >= 10) return whole + suffix;
        int tenth = amount % unit / (unit / 10);
        return tenth == 0 ? whole + suffix : whole + "." + tenth + suffix;
    }
}
