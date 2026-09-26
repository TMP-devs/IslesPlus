package com.islesplus.features.chatfilter;

import com.islesplus.sync.FeatureFlags;

public final class ChatFilter {
    public static boolean chatFilterEnabled = false;
    public static boolean filterManaMeteor = true;
    public static boolean filterGuildChat = false;
    public static boolean filterDeaths = true;

    private static final String MANA_METEOR_PREFIX = "(!!!) A Mana Meteor has crashed down near ";

    private ChatFilter() {}

    /** true = hide this message */
    public static boolean shouldFilter(String plain) {
        if (!chatFilterEnabled || FeatureFlags.isKilled("chat_filter")) return false;

        if (filterManaMeteor && plain.startsWith(MANA_METEOR_PREFIX)) {
            return true;
        }

        if (filterGuildChat && plain.contains("GUILD") && plain.contains("»")) {
            return true;
        }

        if (filterDeaths && isDeathMessage(plain)) {
            return true;
        }

        return false;
    }

    /** Server death broadcast, e.g. "Name DIED! They lost 0 items and 24 coins!" (with skull and
     * arrow symbols around it). Matched on its fixed wording rather than the symbols, and never
     * on a player's own chat line (those contain the "»" name separator), so someone typing
     * "DIED!" in chat is not hidden. */
    static boolean isDeathMessage(String plain) {
        return plain != null && plain.contains(" DIED!") && plain.contains("They lost ") && !plain.contains("»");
    }
}
