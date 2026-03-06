package com.islesplus.features.plushiefinder;

import com.google.gson.*;
import com.islesplus.logging.IslesLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.util.math.Vec3d;

public final class PlushieRepository {
    private static final String URL = "https://tmp-devs.github.io/islesplusjson/plushies.json";
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("islesplus");
    private static final Path CACHE_PATH = DATA_DIR.resolve("plushie_cache.json");
    private static final Path OWNED_PATH = DATA_DIR.resolve("plushie_owned.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AtomicBoolean refreshInFlight = new AtomicBoolean(false);
    private static final long CLOSEST_REFRESH_MS = 1000L;
    private static final double CLOSEST_POS_EPSILON_SQ = 1.0; // 1 block squared

    private static volatile List<PlushieEntry> cachedPlushies = Collections.emptyList();
    /** Thread-safe set of owned plushie numbers. No upper bound - grows with game updates. */
    private static final Set<Integer> owned = ConcurrentHashMap.newKeySet();
    private static long closestComputedAtMs = 0L;
    private static PlushieEntry closestCached = null;
    private static Vec3d closestFromCached = null;
    private static final Object closestCacheLock = new Object();

    private PlushieRepository() {}

    /** Call once on client startup. Loads local data, then fetches fresh data in background. */
    public static void init() {
        loadOwned();
        List<PlushieEntry> local = parseJson(readFile(CACHE_PATH));
        if (local != null) {
            cachedPlushies = List.copyOf(local);
            invalidateClosestCache();
        }
        // Reuse the same gate used by manual refresh to prevent concurrent fetches.
        refreshRemoteDataNowAsync();
    }

    /** Triggers a one-time background refresh from GitHub. Returns false if one is already running. */
    public static boolean refreshRemoteDataNowAsync() {
        if (!refreshInFlight.compareAndSet(false, true)) return false;
        Thread fetcher = new Thread(() -> {
            try {
                fetchAndCache();
            } finally {
                refreshInFlight.set(false);
            }
        }, "RemoteDataRefreshPlushies");
        fetcher.setDaemon(true);
        fetcher.start();
        return true;
    }

    public static List<PlushieEntry> getCachedPlushies() {
        return cachedPlushies;
    }

    public static PlushieEntry getClosestUnowned(Vec3d from) {
        synchronized (closestCacheLock) {
            long now = System.currentTimeMillis();
            boolean sameFrom = closestFromCached != null
                && closestFromCached.squaredDistanceTo(from.x, from.y, from.z) <= CLOSEST_POS_EPSILON_SQ;
            if (sameFrom && now - closestComputedAtMs < CLOSEST_REFRESH_MS) {
                return closestCached;
            }
            closestCached = findClosestUnowned(from);
            closestFromCached = from;
            closestComputedAtMs = now;
            return closestCached;
        }
    }

    public static boolean isOwned(int num) {
        return num >= 0 && owned.contains(num);
    }

    public static void setOwned(int num, boolean value) {
        if (num < 0) return;
        boolean changed = value ? owned.add(num) : owned.remove(num);
        if (!changed) return;
        invalidateClosestCache();
        saveOwned();
    }

    public static void setOwnedBatch(Map<Integer, Boolean> updates) {
        if (updates == null || updates.isEmpty()) return;
        boolean changed = false;
        for (Map.Entry<Integer, Boolean> e : updates.entrySet()) {
            Integer num = e.getKey();
            Boolean value = e.getValue();
            if (num == null || value == null || num < 0) continue;
            boolean c = value ? owned.add(num) : owned.remove(num);
            if (c) changed = true;
        }
        if (!changed) return;
        invalidateClosestCache();
        saveOwned();
    }

    // -------------------------------------------------------------------------

    /** Synchronous gated refresh. Returns true if fetch succeeded, false if failed or already in flight. */
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
                IslesLog.runtimeInfo("[Isles+] PlushieFinder: using cached data (http status: " + status + ")");
                return false;
            }
            String json = response.body();

            List<PlushieEntry> fetched = parseJson(json);
            if (fetched != null) {
                cachedPlushies = List.copyOf(fetched);
                invalidateClosestCache();
                writeFile(CACHE_PATH, json);
                return true;
            }
            return false;
        } catch (Exception e) {
            IslesLog.runtimeInfo("[Isles+] PlushieFinder: using cached data (fetch failed: " + e.getMessage() + ")");
            return false;
        }
    }

    private static List<PlushieEntry> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) return null;
            List<PlushieEntry> plushies = new ArrayList<>();
            for (JsonElement item : root.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                JsonObject plushie = item.getAsJsonObject();

                JsonElement numElement = plushie.get("plushie_num");
                if (numElement == null || numElement.isJsonNull()) continue;
                int num = numElement.getAsInt();

                JsonElement xRealElement = plushie.get("x_real");
                if (xRealElement == null || xRealElement.isJsonNull()) continue;
                JsonElement yRealElement = plushie.get("y_real");
                if (yRealElement == null || yRealElement.isJsonNull()) continue;
                JsonElement zRealElement = plushie.get("z_real");
                if (zRealElement == null || zRealElement.isJsonNull()) continue;

                double xReal = xRealElement.getAsDouble();
                double yReal = yRealElement.getAsDouble();
                double zReal = zRealElement.getAsDouble();
                Double xEntrance = nullableDouble(plushie, "x_entrance");
                Double yEntrance = nullableDouble(plushie, "y_entrance");
                Double zEntrance = nullableDouble(plushie, "z_entrance");
                plushies.add(new PlushieEntry(num, xReal, yReal, zReal, xEntrance, yEntrance, zEntrance));
            }
            return plushies;
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] PlushieFinder: failed to parse JSON", e);
            return null;
        }
    }

    private static Double nullableDouble(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return (element == null || element.isJsonNull()) ? null : element.getAsDouble();
    }

    private static void loadOwned() {
        String raw = readFile(OWNED_PATH);
        if (raw == null) return;
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            if (root.has("owned") && root.get("owned").isJsonArray()) {
                // Legacy format: { "owned": [true, false, ...] }
                JsonArray arr = root.getAsJsonArray("owned");
                for (int i = 0; i < arr.size(); i++) {
                    JsonElement value = arr.get(i);
                    if (value.isJsonPrimitive() && value.getAsBoolean()) owned.add(i);
                }
                return;
            }

            // Sparse format: { "1": true, "5": true, ... } (only true entries saved)
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                int num;
                try {
                    num = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (num < 0) continue;
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsBoolean()) owned.add(num);
            }
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] PlushieFinder: failed to load owned data", e);
        }
    }

    private static void saveOwned() {
        List<Integer> sorted = new ArrayList<>(owned);
        Collections.sort(sorted);
        JsonObject obj = new JsonObject();
        for (int num : sorted) {
            obj.addProperty(Integer.toString(num), true);
        }
        writeFile(OWNED_PATH, GSON.toJson(obj));
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
            IslesLog.runtimeWarn("[Isles+] PlushieFinder: failed to write " + path, e);
        }
    }

    private static double dist2(Vec3d from, double x, double y, double z) {
        double dx = x - from.x;
        double dy = y - from.y;
        double dz = z - from.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static PlushieEntry findClosestUnowned(Vec3d from) {
        PlushieEntry closest = null;
        double best = Double.MAX_VALUE;
        for (PlushieEntry p : cachedPlushies) {
            if (isOwned(p.num)) continue;
            double d = dist2(from, p.xReal, p.yReal, p.zReal);
            if (p.hasEntrance()) {
                d = Math.min(d, dist2(from, p.xEntrance, p.yEntrance, p.zEntrance));
            }
            if (d < best) {
                best = d;
                closest = p;
            }
        }
        return closest;
    }

    private static void invalidateClosestCache() {
        synchronized (closestCacheLock) {
            closestComputedAtMs = 0L;
            closestCached = null;
            closestFromCached = null;
        }
    }
}
