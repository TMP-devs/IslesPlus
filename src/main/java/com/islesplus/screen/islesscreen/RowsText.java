package com.islesplus.screen.islesscreen;

/**
 * The bits of {@link Rows} that are pure text formatting, split out so they can be unit tested
 * without loading {@code Rows} itself (which reaches into every feature class and into
 * {@code MinecraftClient}, and so cannot be touched from a plain JUnit test).
 *
 * <p>No state, no Minecraft, no config: callers pass the already-counted numbers in.
 */
final class RowsText {

    private RowsText() {}

    /** Footnote under the chat-filter tiles, pluralised: "Nothing hidden." / "Hiding 1 type." */
    static String chatFilterSummary(int hidden) {
        if (hidden == 0) return "Nothing hidden.";
        return "Hiding " + hidden + (hidden == 1 ? " type." : " types.");
    }

    /** The "n / total" chip beside a group of tiles. */
    static String countChip(int n, int total) {
        return n + " / " + total;
    }
}
