package com.islesplus.features.rankcalculator;

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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RiftRepository {
    private static final String URL = "https://tmp-devs.github.io/islesplusjson/rifts.json";
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("islesplus");
    private static final Path CACHE_PATH = DATA_DIR.resolve("rift_cache.json");
    private static final AtomicBoolean refreshInFlight = new AtomicBoolean(false);

    private static volatile Map<String, Integer> riftDurations = Collections.emptyMap();
    private static volatile Set<String> plushieRifts = Collections.emptySet();
    private static volatile Set<String> disabledRifts = Collections.emptySet();

    private RiftRepository() {}

    public static void init() {
        ParsedRifts local = parseJson(readFile(CACHE_PATH));
        if (local != null) {
            riftDurations = local.durations();
            plushieRifts = local.plushieRifts();
            disabledRifts = local.disabledRifts();
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
        }, "RemoteDataRefreshRifts");
        fetcher.setDaemon(true);
        fetcher.start();
        return true;
    }

    public static Map<String, Integer> getRiftDurations() {
        return riftDurations;
    }

    public static Set<String> getPlushieRifts() {
        return plushieRifts;
    }

    public static Set<String> getDisabledRifts() {
        return disabledRifts;
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
                IslesLog.runtimeInfo("[Isles+] RiftRepository: using cached data (http status: " + status + ")");
                return false;
            }
            String json = response.body();
            ParsedRifts fetched = parseJson(json);
            if (fetched != null) {
                riftDurations = fetched.durations();
                plushieRifts = fetched.plushieRifts();
                disabledRifts = fetched.disabledRifts();
                writeFile(CACHE_PATH, json);
                return true;
            }
            return false;
        } catch (Exception e) {
            IslesLog.runtimeInfo("[Isles+] RiftRepository: using cached data (fetch failed: " + e.getMessage() + ")");
            return false;
        }
    }

    private record ParsedRifts(Map<String, Integer> durations, Set<String> plushieRifts, Set<String> disabledRifts) {}

    private static ParsedRifts parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            JsonObject rootObj = root.getAsJsonObject();

            Map<String, Integer> durations = new LinkedHashMap<>();
            JsonElement riftsElement = rootObj.get("rifts");
            if (riftsElement != null && riftsElement.isJsonArray()) {
                for (JsonElement riftEntry : riftsElement.getAsJsonArray()) {
                    if (!riftEntry.isJsonObject()) continue;
                    JsonObject rift = riftEntry.getAsJsonObject();
                    JsonElement nameElement = rift.get("name");
                    JsonElement durationElement = rift.get("duration_seconds");
                    if (nameElement == null || durationElement == null) continue;
                    durations.put(nameElement.getAsString().toLowerCase(Locale.ROOT), durationElement.getAsInt());
                }
            }

            Set<String> plushie = parseStringSet(rootObj.get("plushie_rifts"));
            Set<String> disabled = parseStringSet(rootObj.get("disabled_rifts"));

            return new ParsedRifts(
                Collections.unmodifiableMap(durations),
                Collections.unmodifiableSet(plushie),
                Collections.unmodifiableSet(disabled)
            );
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] RiftRepository: failed to parse JSON", e);
            return null;
        }
    }

    private static Set<String> parseStringSet(JsonElement element) {
        Set<String> set = new HashSet<>();
        if (element != null && element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (!item.isJsonPrimitive()) continue;
                set.add(item.getAsString().toLowerCase(Locale.ROOT));
            }
        }
        return set;
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
            IslesLog.runtimeWarn("[Isles+] RiftRepository: failed to write " + path, e);
        }
    }
}
