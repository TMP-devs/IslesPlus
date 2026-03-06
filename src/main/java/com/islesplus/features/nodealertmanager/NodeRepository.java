package com.islesplus.features.nodealertmanager;

import com.google.gson.JsonElement;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NodeRepository {
    private static final String URL = "https://tmp-devs.github.io/islesplusjson/nodes.json";
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("islesplus");
    private static final Path CACHE_PATH = DATA_DIR.resolve("node_cache.json");
    private static final AtomicBoolean refreshInFlight = new AtomicBoolean(false);

    private static final List<String> FALLBACK_NODES = Collections.unmodifiableList(Arrays.asList(
        "Oak Tree",
        "Birch Tree",
        "Fungus Tree",
        "Ash Tree",
        "Palm Tree",
        "Whisper Tree",
        "Tin Ore",
        "Copper Ore",
        "Coal",
        "Salt",
        "Iron Ore",
        "Silver Ore",
        "Nickel Ore",
        "Rhodonite Ore",
        "Rune Essence",
        "Wheat Field",
        "Carrot Field",
        "Tomato Field",
        "Cabbage Field",
        "Wishing Well",
        "Basic Fishing Spot",
        "River Fishing Spot",
        "Beach Fishing Spot",
        "Plains Fishing Spot",
        "Pond Fishing Spot",
        "Warm Beach Fishing Spot",
        "Warm Pond Fishing Spot",
        "Garlic Field",
        "Sugarcane Field",
        "Corn Field"
    ));

    private static volatile List<String> nodeNames = FALLBACK_NODES;

    private NodeRepository() {}

    public static void init() {
        List<String> local = parseJson(readFile(CACHE_PATH));
        if (local != null) nodeNames = local;
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
        }, "RemoteDataRefreshNodes");
        fetcher.setDaemon(true);
        fetcher.start();
        return true;
    }

    public static List<String> getNodeNames() {
        return nodeNames;
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
                IslesLog.runtimeInfo("[Isles+] NodeRepository: using cached data (http status: " + status + ")");
                return false;
            }
            String json = response.body();
            List<String> fetched = parseJson(json);
            if (fetched != null) {
                nodeNames = fetched;
                writeFile(CACHE_PATH, json);
                IslesLog.runtimeInfo("[Isles+] NodeRepository: loaded " + fetched.size() + " nodes");
                return true;
            }
            return false;
        } catch (Exception e) {
            IslesLog.runtimeInfo("[Isles+] NodeRepository: using cached data (fetch failed: " + e.getMessage() + ")");
            return false;
        }
    }

    private static List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonArray()) return null;
            List<String> names = new java.util.ArrayList<>();
            for (JsonElement node : root.getAsJsonArray()) {
                if (!node.isJsonPrimitive()) continue;
                names.add(node.getAsString());
            }
            return Collections.unmodifiableList(names);
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] NodeRepository: failed to parse JSON", e);
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
            IslesLog.runtimeWarn("[Isles+] NodeRepository: failed to write " + path, e);
        }
    }
}
