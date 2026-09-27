package com.islesplus.features.chatchannel;

import com.islesplus.sync.FeatureFlags;

/**
 * State and pure routing logic for the chat channel selector.
 *
 * <p>This class intentionally has no Minecraft GUI or networking dependencies. The chat mixin
 * only changes the argument vanilla is already about to submit, so vanilla remains responsible
 * for normalization, history, command routing, and message signing.</p>
 */
public final class ChatChannels {
    public static final String KILL_KEY = "chat_channels";

    public static boolean chatChannelsEnabled = true;
    public static ChatChannel selectedChannel = ChatChannel.ALL;

    private ChatChannels() {}

    public static boolean active() {
        return chatChannelsEnabled && !FeatureFlags.isKilled(KILL_KEY);
    }

    /**
     * Adds the selected channel command to ordinary text.
     *
     * <p>The checks are ordered so empty input and manually entered commands are never changed,
     * even if a channel is selected.</p>
     */
    public static String route(String chatText) {
        return route(chatText, selectedChannel, active());
    }

    /**
     * Pure routing form used by tests and callers that already evaluated feature availability.
     * It has no Fabric or Minecraft dependency.
     */
    public static String route(String chatText, ChatChannel channel, boolean enabled) {
        if (chatText == null || chatText.isBlank()) return chatText;
        if (chatText.startsWith("/")) return chatText;
        if (!enabled || channel == null || channel == ChatChannel.ALL) return chatText;
        return channel.prefix + chatText;
    }
}