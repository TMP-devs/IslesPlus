package com.islesplus.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Locale;
import java.util.Map;

/**
 * what the remote json says about one feature on the running mod version.
 *
 *   disabled  the feature's code does not run; its card stays, greyed, with a DISABLED chip whose
 *             hover tooltip is {@code disabledReason} ("" = the default text)
 *   killed    the feature's code does not run AND it is removed from the UI entirely. killed
 *             always wins over disabled
 *   tooltip   text for the card's "i" badge. null = not set (the mod's built-in tooltip is used),
 *             "" = no tooltip, anything else = that text
 *   beta      true/false: show or hide the small BETA tag. null = not set (the mod's built-in
 *             choice is used)
 *
 * one feature in the "features" object of features_v2.json:
 *
 *   "chest_finder": {
 *     "disabled": false, "disabled_reason": "",
 *     "killed": false,
 *     "tooltip": "Shaders may break this feature",
 *     "versions": {
 *       "1.0.2":          { "killed": true },
 *       ">=1.0.3 <1.0.5": { "disabled": true, "disabled_reason": "Update to 1.0.5" }
 *     }
 *   }
 *
 * the top-level fields are global: they apply to every version and nothing can undo them. each
 * "versions" entry whose key matches the running version can only ADD to them:
 *   disabled = global disabled OR any matching version's disabled   (same for killed)
 *   a "false" under versions never re-enables a feature the global fields turned off.
 *   disabled_reason = the global one if it is set, else the first non-empty one from a matching
 *                     version entry, in file order (skipping entries that say "disabled": false)
 * tooltip and beta are the exceptions, being display rather than a safety switch: the first
 * matching version entry with a "tooltip" (or "beta") REPLACES the global one on that version
 * ("" hides the tooltip, false hides the tag).
 * a key is a {@link VersionGate} spec ("1.0.2", "<1.0.4", ">=1.0.2 <1.0.5", "*"), or several of
 * them split by commas, any one matching ("1.0.2, 1.0.3").
 *
 * every field is optional: missing = false / "". a field with the wrong type is ignored (as if it
 * were missing), and a malformed version key matches nothing, so mistakes fail open.
 */
public record FeatureState(boolean disabled, String disabledReason, boolean killed, String tooltip, Boolean beta) {
    public static final FeatureState NORMAL = new FeatureState(false, "", false, null, null);

    /** true when the feature's code must not run: disabled or killed */
    public boolean blocked() { return disabled || killed; }

    /** one feature from the "features" object, resolved for {@code version} ("" = unknown, which
     * only the "*" version key matches). anything that is not an object = {@link #NORMAL}. */
    public static FeatureState parse(JsonElement value, String version) {
        if (value == null || !value.isJsonObject()) return NORMAL;
        JsonObject o = value.getAsJsonObject();
        Builder b = new Builder();
        b.apply(o);
        String tooltip = str(o.get("tooltip"));
        String versionTooltip = null;
        Boolean beta = bool(o.get("beta"));
        Boolean versionBeta = null;
        JsonElement versions = o.get("versions");
        if (versions != null && versions.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : versions.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonObject() && keyMatches(entry.getKey(), version)) {
                    JsonObject v = entry.getValue().getAsJsonObject();
                    b.apply(v);
                    if (versionTooltip == null) versionTooltip = str(v.get("tooltip"));
                    if (versionBeta == null) versionBeta = bool(v.get("beta"));
                }
            }
        }
        return new FeatureState(b.disabled, b.disabledReason, b.killed,
            versionTooltip != null ? versionTooltip : tooltip,
            versionBeta != null ? versionBeta : beta);
    }

    /** one entry of the old "killed" object (still in features.json and read as a fallback): a
     * {@link VersionGate} rule that, when it matches, disables the feature with the default tooltip */
    public static FeatureState legacy(JsonElement rule, String version) {
        return VersionGate.matches(rule, version) ? new FeatureState(true, "", false, null, null) : NORMAL;
    }

    /** the tooltip to show: {@code remote} (the json's text) when set, else {@code builtIn}.
     * null = no tooltip (remote "" or no built-in text) */
    static String resolveTooltip(String remote, String builtIn) {
        String text = remote != null ? remote : builtIn;
        return text == null || text.isBlank() ? null : text;
    }

    /** "1.0.2, >=1.1": any comma-separated spec matching is enough */
    static boolean keyMatches(String key, String version) {
        for (String spec : key.split(",")) {
            if (!spec.isBlank() && VersionGate.matchesSpec(spec, version)) return true;
        }
        return false;
    }

    private static final class Builder {
        boolean disabled, killed;
        String disabledReason = "";

        /** the global object first, then matching version entries: flags only ever turn on, and
         * the first non-empty disabled_reason sticks */
        void apply(JsonObject o) {
            Boolean d = bool(o.get("disabled"));
            if (Boolean.TRUE.equals(d)) disabled = true;
            if (Boolean.TRUE.equals(bool(o.get("killed")))) killed = true;
            // an entry that says "disabled": false (which cannot undo a global true) does not get
            // to supply the tooltip either
            String dr = str(o.get("disabled_reason"));
            if (disabledReason.isEmpty() && dr != null && !Boolean.FALSE.equals(d)) disabledReason = dr;
        }
    }

    /** true/false, also as the strings "true"/"false"; null for anything else */
    private static Boolean bool(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return null;
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isBoolean()) return p.getAsBoolean();
        if (p.isString()) {
            String s = p.getAsString().trim().toLowerCase(Locale.ROOT);
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
        }
        return null;
    }

    /** a string (trimmed); null for anything else */
    private static String str(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) return null;
        return e.getAsString().trim();
    }
}
