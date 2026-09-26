package com.islesplus.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islesplus.logging.IslesLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * remote kill switch so we can turn features off for everyone if the server changes something.
 * pulls features_v2.json from github, and only believes it when features_v2.json.sig checks out
 * against the key built into the mod ({@link SignedFlags}): a copy someone edited by hand, on
 * disk or on the way, is ignored. the last good (signed) copy is kept on disk and used until a
 * newer good one arrives, so blocking the download only freezes the flags as they were - it no
 * longer switches everything back on. only a player who never had a good copy runs with
 * everything on. an older signed file than the one we have ("issued") is refused too.
 *
 * older mods keep working as before: they read features_v2.json (ignoring "issued" and the .sig)
 * or the old features.json, and their own features_cache.json. this build does not touch either.
 *
 * v2 has a "features" object: per feature, "disabled" (greyed card + tooltip), "killed"
 * (removed from the UI, beats disabled), the card's "i" "tooltip" and its "beta" tag, with
 * overrides per mod version. see {@link FeatureState}.
 * the old "killed" object (version rules, see {@link VersionGate}) is still read, because the
 * legacy features.json fallback only has that; a match there = disabled. a feature listed in both
 * goes by "features".
 *
 * feature keys:
 *   harvest_timer, node_radius, node_depleted_ping, regen_mode,
 *   drop_notify, inventory_full, vending_machine_finder, chest_finder,
 *   plushie_finder, button_finder, mob_finder, player_finder,
 *   rank_calculator, inventory_search, chat_filter, waystone_finder,
 *   qte_tracker (whole thing) plus per type: qte_tracker_luck, qte_tracker_exp,
 *   qte_tracker_chance, qte_tracker_coins, qte_tracker_tickskip,
 *   roll_percent, item_age
 *   quick_actions (the inventory buttons)
 *   berry_alert, treasure_chests, food_buff_timer, void_rift_timer, egg_timer, storage_count
 *   announcements (json only, no card: stops fetching and showing /announce messages)
 */
public final class FeatureFlags {
    /** one client for every fetch: a new one each time would each keep a thread until GC */
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final String URL = "https://tmp-devs.github.io/islesplusjson/features_v2.json";
    private static final String SIG_URL = URL + ".sig";
    /** running mod version, what the version keys and rules are checked against */
    private static final String MOD_VERSION = FabricLoader.getInstance().getModContainer("islesplus")
        .map(c -> c.getMetadata().getVersion().getFriendlyString())
        .orElse("");
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("islesplus");
    /** the last features_v2.json whose signature checked out, with that signature. (not the
     * features_cache.json older builds keep: that one is unsigned, and left alone for them.) */
    private static final Path VERIFIED_CACHE_PATH = DATA_DIR.resolve("features_v2_verified.json");
    /** when the flags in use were issued; nothing older is accepted after them */
    private static volatile java.time.Instant issued = java.time.Instant.EPOCH;
    private static final AtomicBoolean refreshInFlight = new AtomicBoolean(false);

    /** features that are currently disabled or killed; a feature missing here runs normally */
    private static volatile Map<String, FeatureState> blocked = Map.of();

    /** remote "i" badge text per feature ("" = none). a feature missing here uses its built-in text */
    private static volatile Map<String, String> tooltips = Map.of();

    /** remote BETA tag per feature (true = show, false = hide). a feature missing here uses its built-in choice */
    private static volatile Map<String, Boolean> betas = Map.of();

    /** message to show in chat on join, "" = nothing */
    private static volatile String motd = "";

    /** One styled line of the join message, from "motd_v2" in features.json. {@code link},
     * {@code linkText} and {@code color} ("#rrggbb") may be empty. */
    public record MotdLine(String text, String link, String linkText, String color) {}

    /** styled join lines; empty = fall back to the plain {@link #motd} string */
    private static volatile java.util.List<MotdLine> motdLines = java.util.List.of();

    /** newest mod version according to remote, "" = dunno */
    private static volatile String latestVersion = "";
    /** direct download link for it, "" = just point at the modrinth page */
    private static volatile String latestVersionUrl = "";

    private FeatureFlags() {}

    /** local override: when true isKilled() and isHidden() are always false. only honoured while
     * the companion tools are installed (they set this field; recognised by the hash of their mod
     * id), so another mod flipping it does nothing. */
    public static volatile boolean localOverride = false;
    private static final String COMPANION_ID_SHA256 = "8666a4d9d713007978c67dce20c8db4ebf3d9ab0f55c8ee1384044f6804d0dba";
    private static final boolean COMPANION_PRESENT = companionPresent();

    private static boolean companionPresent() {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            for (net.fabricmc.loader.api.ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                byte[] d = md.digest(mod.getMetadata().getId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (java.util.HexFormat.of().formatHex(d).equals(COMPANION_ID_SHA256)) return true;
            }
        } catch (java.security.NoSuchAlgorithmException ignored) {}
        return false;
    }

    private static boolean bypass() { return localOverride && COMPANION_PRESENT; }

    /** true when the feature's code must not run: it is disabled OR killed in the json. (the name
     * predates the json's "killed" field, which is the narrower {@link #isHidden}.) */
    public static boolean isKilled(String key) {
        return !bypass() && blocked.containsKey(key);
    }

    /** true when the feature is "killed" in the json: removed from the UI entirely, no tooltip. */
    public static boolean isHidden(String key) {
        if (bypass()) return false;
        FeatureState s = blocked.get(key);
        return s != null && s.killed();
    }

    /** the "i" badge text for a feature card: the json's tooltip for {@code key} when it sets one,
     * else {@code builtIn}. null = no badge (json "" or no built-in text). not affected by the
     * dev bypass. */
    public static String tooltip(String key, String builtIn) {
        return FeatureState.resolveTooltip(key == null ? null : tooltips.get(key), builtIn);
    }

    /** whether a feature (card or option tile) shows its BETA tag: the json's "beta" for {@code key}
     * when it sets one, else {@code builtIn}. not affected by the local override. */
    public static boolean beta(String key, boolean builtIn) {
        Boolean remote = key == null ? null : betas.get(key);
        return remote != null ? remote : builtIn;
    }

    /** tooltip for a disabled feature, "" = none given (use the default text) */
    public static String disabledReason(String key) {
        FeatureState s = blocked.get(key);
        return s == null ? "" : s.disabledReason();
    }

    public static String getMotd() {
        return motd;
    }

    public static java.util.List<MotdLine> getMotdLines() {
        return motdLines;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getLatestVersionUrl() {
        return latestVersionUrl;
    }

    public static void init() {
        String[] cached = readVerifiedCache();
        if (cached != null && SignedFlags.verify(cached[0], cached[1])) {
            ParsedFlags flags = parseJson(cached[0]);
            if (flags != null) apply(flags, SignedFlags.issued(cached[0]));
        } else if (cached != null) {
            IslesLog.runtimeWarn("[Isles+] FeatureFlags: saved flags failed the signature check, ignoring them");
        }
        refreshRemoteDataNowAsync();
    }

    private static void apply(ParsedFlags flags, java.time.Instant when) {
        blocked = flags.blocked;
        tooltips = flags.tooltips;
        betas = flags.betas;
        motd = flags.motd;
        motdLines = flags.motdLines;
        latestVersion = flags.latestVersion;
        latestVersionUrl = flags.latestVersionUrl;
        issued = when;
    }

    public static boolean refreshRemoteDataNowAsync() {
        if (!refreshInFlight.compareAndSet(false, true)) return false;
        Thread fetcher = new Thread(() -> {
            try {
                fetchAndCache();
            } finally {
                refreshInFlight.set(false);
            }
        }, "RemoteDataRefreshFeatureFlags");
        fetcher.setDaemon(true);
        fetcher.start();
        return true;
    }

    public static boolean refreshSync() {
        if (!refreshInFlight.compareAndSet(false, true)) return false;
        try {
            return fetchAndCache();
        } finally {
            refreshInFlight.set(false);
        }
    }

    /** fetches features_v2.json and its signature. on success (signed by us, not older than what we
     * have, parses) the flags switch over and the pair is saved; on anything else the flags in use
     * stay exactly as they are - never cleared - and false is returned. */
    public static boolean fetchAndCache() {
        String json = fetch(URL);
        String sig = json == null ? null : fetch(SIG_URL);
        if (json == null || sig == null) {
            IslesLog.runtimeInfo("[Isles+] FeatureFlags: could not download the flags, keeping the ones in use");
            return false;
        }
        if (!SignedFlags.verify(json, sig)) {
            IslesLog.runtimeWarn("[Isles+] FeatureFlags: features_v2.json failed the signature check, keeping the flags in use");
            return false;
        }
        java.time.Instant when = SignedFlags.issued(json);
        if (when.isBefore(issued)) {
            IslesLog.runtimeWarn("[Isles+] FeatureFlags: got flags issued " + when + ", older than the ones in use (" + issued + "); ignoring");
            return false;
        }
        ParsedFlags flags = parseJson(json);
        if (flags == null) return false;
        apply(flags, when);
        writeVerifiedCache(json, sig);
        return true;
    }

    private static String[] readVerifiedCache() {
        String text = readFile(VERIFIED_CACHE_PATH);
        if (text == null) return null;
        try {
            JsonObject o = JsonParser.parseString(text).getAsJsonObject();
            return new String[] {o.get("json").getAsString(), o.get("sig").getAsString()};
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void writeVerifiedCache(String json, String sig) {
        JsonObject o = new JsonObject();
        o.addProperty("json", json);
        o.addProperty("sig", sig.trim());
        writeFile(VERIFIED_CACHE_PATH, o.toString());
    }

    /** body of a 2xx response, null on any failure */
    private static String fetch(String url) {
        try {
            HttpClient client = HTTP;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                IslesLog.runtimeInfo("[Isles+] FeatureFlags: " + url + " gave http status " + status);
                return null;
            }
            return response.body();
        } catch (Exception e) {
            IslesLog.runtimeInfo("[Isles+] FeatureFlags: " + url + " fetch failed: " + e.getMessage());
            return null;
        }
    }

    private record ParsedFlags(Map<String, FeatureState> blocked, Map<String, String> tooltips, Map<String, Boolean> betas, String motd, java.util.List<MotdLine> motdLines, String latestVersion, String latestVersionUrl) {}

    /** "motd_v2": one line object, or an array of them. Anything malformed is skipped. */
    static java.util.List<MotdLine> parseMotdLines(JsonElement el) {
        java.util.List<MotdLine> lines = new java.util.ArrayList<>();
        if (el == null) return lines;
        Iterable<JsonElement> items = el.isJsonArray() ? el.getAsJsonArray() : java.util.List.of(el);
        for (JsonElement item : items) {
            if (!item.isJsonObject()) continue;
            JsonObject o = item.getAsJsonObject();
            String text = str(o, "text");
            if (text.isBlank()) continue;
            lines.add(new MotdLine(text, str(o, "link"), str(o, "link_text"), str(o, "color")));
        }
        return java.util.List.copyOf(lines);
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString().trim() : "";
    }

    private static ParsedFlags parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            JsonObject rootObj = root.getAsJsonObject();

            // at least one of the two must be there, otherwise this is not a features file
            JsonElement killedElement = rootObj.get("killed");
            JsonElement featuresElement = rootObj.get("features");
            boolean hasKilled = killedElement != null && killedElement.isJsonObject();
            boolean hasFeatures = featuresElement != null && featuresElement.isJsonObject();
            if (!hasKilled && !hasFeatures) return null;
            Map<String, FeatureState> states = new HashMap<>();
            if (hasKilled) {
                for (var entry : killedElement.getAsJsonObject().entrySet()) {
                    states.put(entry.getKey(), FeatureState.legacy(entry.getValue(), MOD_VERSION));
                }
            }
            if (hasFeatures) {
                for (var entry : featuresElement.getAsJsonObject().entrySet()) {
                    states.put(entry.getKey(), FeatureState.parse(entry.getValue(), MOD_VERSION));
                }
            }
            Map<String, FeatureState> blockedKeys = new HashMap<>();
            Map<String, String> tooltipTexts = new HashMap<>();
            Map<String, Boolean> betaFlags = new HashMap<>();
            states.forEach((key, state) -> {
                if (state.blocked()) blockedKeys.put(key, state);
                if (state.tooltip() != null) tooltipTexts.put(key, state.tooltip());
                if (state.beta() != null) betaFlags.put(key, state.beta());
            });

            String parsedMotd = "";
            JsonElement motdElement = rootObj.get("motd");
            if (motdElement != null && motdElement.isJsonPrimitive()) {
                parsedMotd = motdElement.getAsString();
            }

            String parsedLatestVersion = "";
            String parsedLatestVersionUrl = "";
            JsonElement versionElement = rootObj.get("latest_version");
            if (versionElement != null && versionElement.isJsonPrimitive()) {
                String raw = versionElement.getAsString().trim();
                int space = raw.indexOf(' ');
                if (space > 0) {
                    parsedLatestVersion = raw.substring(0, space);
                    parsedLatestVersionUrl = raw.substring(space + 1).trim();
                } else {
                    parsedLatestVersion = raw;
                }
            }

            return new ParsedFlags(Map.copyOf(blockedKeys), Map.copyOf(tooltipTexts), Map.copyOf(betaFlags), parsedMotd, parseMotdLines(rootObj.get("motd_v2")), parsedLatestVersion, parsedLatestVersionUrl);
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] FeatureFlags: failed to parse JSON", e);
            return null;
        }
    }

    private static String readFile(Path path) {
        if (!Files.exists(path)) return null;
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeFile(Path path, String content) {
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(path, content);
        } catch (IOException e) {
            IslesLog.runtimeWarn("[Isles+] FeatureFlags: failed to write " + path, e);
        }
    }
}
