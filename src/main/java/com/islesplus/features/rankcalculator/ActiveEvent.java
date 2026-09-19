package com.islesplus.features.rankcalculator;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** The server-wide event shown in the tab list. Only Rift Spelunker changes the score calculation. */
public enum ActiveEvent {
    NONE(""),
    FARMING_FRENZY("farming frenzy"),
    WOODCUTTING_FRENZY("woodcutting frenzy"),
    MINING_FRENZY("mining frenzy"),
    FISHING_FRENZY("fishing frenzy"),
    BOSS_BIAS("boss bias"),
    RIFT_SPELUNKER("rift spelunker"),
    DUNGEON_DROPPER("dungeon dropper"),
    MINION_FRENZY("minion frenzy"),
    PET_SHAREHOLDER("pet shareholder");

    /** Strips Minecraft color/format codes (§ + any char). */
    private static final Pattern STRIP_COLOR = Pattern.compile("§.");

    private final String label;

    ActiveEvent(String label) {
        this.label = label;
    }

    /** Rift Spelunker: rift grades require 10% less score. */
    public double thresholdScale() {
        return this == RIFT_SPELUNKER ? 0.9 : 1.0;
    }

    private static final String ANNOUNCEMENT_PREFIX = "current event:";

    /** How long a chat announcement is trusted when the tab list names no event. */
    static final long ANNOUNCEMENT_TTL_MS = 15L * 60L * 1000L;

    /** The event to score with: whatever the tab list names, otherwise a recent chat announcement. */
    public static ActiveEvent resolve(ActiveEvent fromTabList, ActiveEvent announced, long announcementAgeMs) {
        if (fromTabList != null && fromTabList != NONE) return fromTabList;
        if (announced != null && announced != NONE && announcementAgeMs >= 0 && announcementAgeMs <= ANNOUNCEMENT_TTL_MS) {
            return announced;
        }
        return NONE;
    }

    /** The server's periodic chat broadcast, e.g. "CURRENT EVENT: Rift Spelunker". Returns the
     * event it names (NONE if it names one we do not know), or null if the line is not that
     * broadcast at all.
     * <p>The line has to START with the phrase - after colour codes and any leading bullet or
     * bracket characters. A player cannot forge that: their chat, party and private messages all
     * begin with a name or a channel tag, so "Bob >> current event: rift spelunker" (with the chat arrow) is rejected
     * while a server line such as "- CURRENT EVENT: ..." (any leading bullet) is accepted. */
    public static ActiveEvent fromAnnouncement(String chatLine) {
        if (chatLine == null) return null;
        String line = STRIP_COLOR.matcher(chatLine).replaceAll("").toLowerCase(Locale.ROOT);
        int start = 0;
        while (start < line.length() && !Character.isLetterOrDigit(line.charAt(start))) start++;
        if (!line.startsWith(ANNOUNCEMENT_PREFIX, start)) return null;
        return detect(List.of(line.substring(start)));
    }

    /** Returns the first event named anywhere in the given tab-list text, or NONE. */
    public static ActiveEvent detect(List<String> tabLines) {
        for (String raw : tabLines) {
            String line = STRIP_COLOR.matcher(raw).replaceAll("").toLowerCase(Locale.ROOT);
            for (ActiveEvent event : values()) {
                if (event != NONE && line.contains(event.label)) return event;
            }
        }
        return NONE;
    }
}
