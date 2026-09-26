package com.islesplus.features.rollpercent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rolls remembered by tooltip text, for items the server sends WITHOUT their roll data: with a
 * cosmetic on, a worn helmet arrives as a stand-in whose lore is intact but whose custom data is
 * just {@code {p_hat:1}}. The real item, seen once (cosmetic off, or in the inventory), teaches
 * the cache each stat line's roll; the stand-in shows the same lines, so it gets the same rolls.
 * <p>Keying by text is sound because the number on a line is itself derived from the roll: the
 * same item at the same level showing the same "+8.9 Max Health" has the same roll, to rounding.
 */
final class RollCache {
    private final int capacity;
    private final LinkedHashMap<String, Double> rolls;
    private boolean dirty;

    RollCache(int capacity) {
        this.capacity = capacity;
        this.rolls = new LinkedHashMap<>(64, 0.75f, false) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
                return size() > RollCache.this.capacity;
            }
        };
    }

    /** What tells one item's lines from another's: its name and its level header. */
    static String item(String name, String header) {
        return name.strip() + "|" + header.strip();
    }

    void remember(String item, String line, double ratio) {
        Double old = rolls.put(key(item, line), ratio);
        if (old == null || old != ratio) dirty = true;
    }

    /** The roll last seen for this line of this item, or null. */
    Double recall(String item, String line) {
        return rolls.get(key(item, line));
    }

    boolean dirty() { return dirty; }

    void markSaved() { dirty = false; }

    String toJson() {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Double> e : rolls.entrySet()) obj.addProperty(e.getKey(), e.getValue());
        return obj.toString();
    }

    /** Adds what a saved cache holds; anything unreadable is skipped, a broken file loads as nothing. */
    void readJson(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return;
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                JsonElement v = e.getValue();
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) rolls.put(e.getKey(), v.getAsDouble());
            }
        } catch (RuntimeException ignored) {
            // a corrupt cache is only a cache: start empty
        }
    }

    private static String key(String item, String line) {
        return item + "|" + line.strip();
    }
}
