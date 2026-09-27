package com.islesplus.features.chatchannel;

/**
 * The chat channels that can be selected while the vanilla chat screen is open.
 *
 * <p>{@link #ALL} deliberately has no prefix: it leaves ordinary chat exactly as vanilla
 * handles it. The other prefixes are sent through the server's normal chat commands.</p>
 */
public enum ChatChannel {
    ALL("Normal", ""),
    PARTY("Party", "/pc "),
    GUILD("Guild", "/gc ");

    public final String label;
    public final String prefix;

    ChatChannel(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    public static ChatChannel fromStoredName(String name) {
        if (name == null || name.isBlank()) return ALL;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }
}