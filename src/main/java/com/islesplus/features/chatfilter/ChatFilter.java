package com.islesplus.features.chatfilter;

import com.islesplus.sync.FeatureFlags;

public final class ChatFilter {
    public static boolean chatFilterEnabled = false;
    public static boolean filterManaMeteor = true;
    public static boolean filterGuildChat = false;

    private static final String MANA_METEOR_PREFIX = "(!!!) A Mana Meteor has crashed down near ";

    private ChatFilter() {}

    /**
     * Returns true if the message should be suppressed (filtered out).
     */
    public static boolean shouldFilter(String plain) {
        if (!chatFilterEnabled || FeatureFlags.isKilled("chat_filter")) return false;

        if (filterManaMeteor && plain.startsWith(MANA_METEOR_PREFIX)) {
            return true;
        }

        if (filterGuildChat && plain.contains("GUILD") && plain.contains("»")) {
            return true;
        }

        return false;
    }
}
