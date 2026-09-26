package com.islesplus.features.quickactions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

/** One quick action button: the item it shows and what clicking it does. */
public final class QuickAction {
    public enum Type {
        /** not set up yet: the button shows a "+" and clicking it opens the editor */
        NONE,
        /** an Isles+ action, {@link #target} is an {@link IslesAction} id */
        ISLES,
        /** press any keybind (vanilla or another mod's), {@link #target} is the keybind id ("key.jump") */
        KEYBIND,
        /** run a chat command, {@link #target} is the command without the leading slash */
        COMMAND
    }

    /** item id shown on the button ("minecraft:ender_pearl"); "" = the default icon for the type */
    public String icon = "";
    public Type type = Type.NONE;
    public String target = "";

    public boolean isSet() {
        return type != Type.NONE && !target.isBlank();
    }

    public void clear() {
        icon = "";
        type = Type.NONE;
        target = "";
    }

    /** "/warp spawn " -> "warp spawn". What gets sent, and what gets saved. */
    public static String normalizeCommand(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        while (s.startsWith("/")) s = s.substring(1).trim();
        return s;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("icon", icon);
        o.addProperty("type", type.name().toLowerCase(Locale.ROOT));
        o.addProperty("target", target);
        return o;
    }

    /** anything missing or unknown loads as an empty button, never an exception */
    public static QuickAction fromJson(JsonElement el) {
        QuickAction a = new QuickAction();
        if (el == null || !el.isJsonObject()) return a;
        JsonObject o = el.getAsJsonObject();
        a.icon = str(o, "icon");
        a.target = str(o, "target");
        try {
            a.type = Type.valueOf(str(o, "type").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            a.type = Type.NONE;
        }
        if (a.type == Type.COMMAND) a.target = normalizeCommand(a.target);
        if (a.type == Type.NONE) a.target = "";
        return a;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().trim() : "";
    }
}
