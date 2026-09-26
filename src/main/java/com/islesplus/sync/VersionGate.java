package com.islesplus.sync;

import com.google.gson.JsonElement;

/**
 * decides whether one "killed" entry in features_v2.json applies to the running mod version.
 *
 * an entry is one of:
 *   true / false        killed on every version / on none
 *   "1.0.2"             exactly that version
 *   "<1.0.3"            comparison: <, <=, >, >=, = (also "*" = every version)
 *   ">=1.0.2 <1.0.5"    several conditions separated by spaces, all must match (a range)
 *   ["1.0.2", ">=1.1"]  a list, any one matching is enough
 *
 * versions compare by their numeric parts, so "1.0" == "1.0.0" and "1.0.10" > "1.0.9". anything
 * after the numbers ("-beta", "+1.21.11") is ignored. a malformed rule never matches, so a typo
 * fails open like the rest of the kill switch.
 */
public final class VersionGate {
    private VersionGate() {}

    /** true when {@code rule} kills the feature on {@code version}. {@code version} "" = unknown,
     * which only unconditional kills (true, "*") match. */
    public static boolean matches(JsonElement rule, String version) {
        if (rule == null || rule.isJsonNull()) return false;
        if (rule.isJsonArray()) {
            for (JsonElement item : rule.getAsJsonArray()) {
                if (item.isJsonPrimitive() && matchesSpec(item.getAsString(), version)) return true;
            }
            return false;
        }
        if (!rule.isJsonPrimitive()) return false;
        if (rule.getAsJsonPrimitive().isBoolean()) return rule.getAsBoolean();
        return matchesSpec(rule.getAsString(), version);
    }

    /** one spec string: space-separated conditions, all of which must hold */
    static boolean matchesSpec(String spec, String version) {
        String s = spec.trim();
        if (s.isEmpty()) return false;
        if (s.equals("*")) return true;
        int[] current = parse(version);
        if (current == null) return false;
        for (String cond : s.split("\\s+")) {
            if (!matchesCondition(cond, current)) return false;
        }
        return true;
    }

    private static boolean matchesCondition(String cond, int[] current) {
        String op = "";
        if (cond.startsWith("<=") || cond.startsWith(">=")) op = cond.substring(0, 2);
        else if (cond.startsWith("<") || cond.startsWith(">") || cond.startsWith("=")) op = cond.substring(0, 1);
        int[] target = parse(cond.substring(op.length()));
        if (target == null) return false;
        int c = compare(current, target);
        return switch (op) {
            case "<" -> c < 0;
            case "<=" -> c <= 0;
            case ">" -> c > 0;
            case ">=" -> c >= 0;
            default -> c == 0;
        };
    }

    /** "1.0.2-beta" -> {1, 0, 2}; null when it doesn't start with a number */
    static int[] parse(String version) {
        if (version == null) return null;
        String v = version.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        int end = 0;
        while (end < v.length() && (Character.isDigit(v.charAt(end)) || v.charAt(end) == '.')) end++;
        String core = v.substring(0, end);
        while (core.endsWith(".")) core = core.substring(0, core.length() - 1);
        if (core.isEmpty() || core.startsWith(".") || core.contains("..")) return null;
        String[] parts = core.split("\\.");
        int[] out = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }

    /** Whether {@code latest} is a newer version than {@code current}. False when either cannot be
     * read, so a typo in the json never nags anyone to "update" to an older or garbled version. */
    public static boolean isNewer(String latest, String current) {
        int[] l = parse(latest), c = parse(current);
        return l != null && c != null && compare(l, c) > 0;
    }

    static int compare(int[] a, int[] b) {
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }
}
