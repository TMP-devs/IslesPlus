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

    /** Whether a card matches the /ip search: every word of {@code query} appears somewhere in its
     * title or description (any case, any order). A blank query matches everything. */
    static boolean matchesSearch(String title, String description, String query) {
        if (query == null || query.isBlank()) return true;
        String hay = ((title == null ? "" : title) + " " + (description == null ? "" : description))
            .toLowerCase(java.util.Locale.ROOT);
        for (String word : query.toLowerCase(java.util.Locale.ROOT).trim().split("\\s+")) {
            if (!hay.contains(word)) return false;
        }
        return true;
    }

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
