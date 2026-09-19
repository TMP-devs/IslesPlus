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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * remote kill switch so we can turn features off for everyone if the server changes something.
 * pulls features.json from github. fails open: if the fetch dies or the json is broken,
 * nothing gets killed.
 *
 * feature keys:
 *   harvest_timer, node_radius, node_depleted_ping, regen_mode,
 *   drop_notify, inventory_full, vending_machine_finder, chest_finder,
 *   plushie_finder, button_finder, mob_finder, player_finder,
 *   rank_calculator, inventory_search, chat_filter, waystone_finder,
 *   qte_tracker (whole thing) plus per type: qte_tracker_luck, qte_tracker_exp,
 *   qte_tracker_chance, qte_tracker_coins, qte_tracker_tickskip
 */
public final class FeatureFlags {
    private static final String URL = "https://tmp-devs.github.io/islesplusjson/features.json";
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("islesplus");
    private static final Path CACHE_PATH = DATA_DIR.resolve("features_cache.json");
    private static final AtomicBoolean refreshInFlight = new AtomicBoolean(false);

    /** feature keys that are currently killed. empty = everything allowed */
    private static volatile Set<String> killed = Collections.emptySet();

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

    /** dev bypass, when true isKilled() is always false */
    public static volatile boolean devBypassKills = false;

    public static boolean isKilled(String key) {
        return !devBypassKills && killed.contains(key);
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
        ParsedFlags cached = parseJson(readFile(CACHE_PATH));
        if (cached != null) {
            killed = cached.killed;
            motd = cached.motd;
            motdLines = cached.motdLines;
            latestVersion = cached.latestVersion;
            latestVersionUrl = cached.latestVersionUrl;
        }
        refreshRemoteDataNowAsync();
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

    public static boolean fetchAndCache() {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                IslesLog.runtimeInfo("[Isles+] FeatureFlags: all features enabled (http status: " + status + ")");
                killed = Collections.emptySet();
                motd = "";
                motdLines = java.util.List.of();
                latestVersion = "";
                latestVersionUrl = "";
                return false;
            }
            String json = response.body();
            ParsedFlags flags = parseJson(json);
            if (flags != null) {
                killed = flags.killed;
                motd = flags.motd;
                motdLines = flags.motdLines;
                latestVersion = flags.latestVersion;
                latestVersionUrl = flags.latestVersionUrl;
                writeFile(CACHE_PATH, json);
                return true;
            } else {
                killed = Collections.emptySet();
                motd = "";
                motdLines = java.util.List.of();
                latestVersion = "";
                latestVersionUrl = "";
                return false;
            }
        } catch (Exception e) {
            IslesLog.runtimeInfo("[Isles+] FeatureFlags: all features enabled (fetch failed: " + e.getMessage() + ")");
            killed = Collections.emptySet();
            motd = "";
                motdLines = java.util.List.of();
            latestVersion = "";
            latestVersionUrl = "";
            return false;
        }
    }

    private record ParsedFlags(Set<String> killed, String motd, java.util.List<MotdLine> motdLines, String latestVersion, String latestVersionUrl) {}

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

            JsonElement killedElement = rootObj.get("killed");
            if (killedElement == null || !killedElement.isJsonObject()) return null;
            Set<String> killedKeys = new HashSet<>();
            for (var entry : killedElement.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsBoolean()) {
                    killedKeys.add(entry.getKey());
                }
            }

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

            return new ParsedFlags(Collections.unmodifiableSet(killedKeys), parsedMotd, parseMotdLines(rootObj.get("motd_v2")), parsedLatestVersion, parsedLatestVersionUrl);
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
