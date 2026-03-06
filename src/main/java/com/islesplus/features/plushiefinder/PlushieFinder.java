package com.islesplus.features.plushiefinder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlushieFinder {
    private static final Pattern STRIP_CODES = Pattern.compile("§.");
    /** Anchored to start of message so guild/player chat prefixes can't spoof it. */
    private static final Pattern FOUND_PATTERN =
        Pattern.compile("(?i)^you\\s+found\\s+plushy\\s*#?(\\d+).*agility\\s+exp");
    public static boolean plushieFinderEnabled = false;

    private PlushieFinder() {}

    /** Called on every incoming chat/game message; marks plushie as owned if the message matches. */
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
