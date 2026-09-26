package com.islesplus.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One message pushed to every mod user with /announce in the Isles+ Discord (see
 * {@link AnnouncementFeed}). Meant for the times we turn a feature off and owe people an
 * explanation, so it lands in chat mid session rather than waiting for the next join like the motd.
 *
 * {@code id} is the whole show-once decision: the client remembers the last id it showed, so
 * re-sending the same text under a new id shows it again. {@code link}, {@code linkText} and
 * {@code color} ("#rrggbb") may be empty.
 */
public record Announcement(String id, String text, String link, String linkText, String color) {

    /** the announcement in a verified payload, or null when it has no id or text */
    public static Announcement parse(JsonElement el) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        String id = str(o, "id");
        String text = str(o, "text");
        if (id.isBlank() || text.isBlank()) return null;
        return new Announcement(id, text, str(o, "link"), str(o, "link_text"), str(o, "color"));
    }

    /** true when this is not the announcement the client last showed */
    public boolean isUnseen(String lastShownId) {
        return !id.equals(lastShownId);
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().trim() : "";
    }
}
