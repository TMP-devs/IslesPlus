package com.islesplus.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islesplus.logging.IslesLog;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Polls a lightweight version file on GitHub Pages to detect remote data changes.
 * When the version bumps, invokes {@link RemoteDataSync#refreshAllSync()} on this
 * background thread, performing a blocking refresh of all remote data.
 *
 * Only polls when the player is in {@link PlayerWorld#ISLE} or {@link PlayerWorld#OTHER}
 * (not during dungeon runs).
 */
public final class RefreshPoller {
    private static final String URL = "https://tmp-devs.github.io/islesplusjson/refresh.json";
    private static final long POLL_INTERVAL_MS = 60_000L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    private static volatile int lastKnownVersion = -1;
    private static volatile String lastEtag = "";
    private static volatile Thread pollerThread;

    private RefreshPoller() {}

    /** Start polling. Called after world detection confirms a safe world. */
    public static void start() {
        stop();
        Thread t = new Thread(RefreshPoller::pollLoop, "IslesPlus-RefreshPoller");
        t.setDaemon(true);
        pollerThread = t;
        t.start();
    }

    /** Stop polling. Called on disconnect. */
    public static void stop() {
        Thread t = pollerThread;
        if (t != null) {
            t.interrupt();
            pollerThread = null;
        }
    }

    /** Trigger an immediate forced refresh (e.g., after leaving a dungeon). */
    public static void forceRefresh() {
        Thread checker = new Thread(() -> checkOnce(true), "IslesPlus-RefreshCheck");
        checker.setDaemon(true);
        checker.start();
    }

    private static boolean isSafeWorld(PlayerWorld world) {
        return world == PlayerWorld.ISLE || world == PlayerWorld.OTHER;
    }

    private static void pollLoop() {
        // Initial check on connect (poller only starts in safe worlds)
        checkOnce(false);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (isSafeWorld(WorldIdentification.world)) {
                checkOnce(false);
            }
        }
    }

    private static synchronized void checkOnce(boolean force) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(8))
                .GET();

            // Skip ETag when forced so we always get a full response
            if (!force) {
                String etag = lastEtag;
                if (!etag.isEmpty()) {
                    reqBuilder.header("If-None-Match", etag);
                }
            }

            HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 304) {
                return; // no change
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }

            String body = response.body();
            if (body == null || body.isBlank()) return;

            JsonElement el = JsonParser.parseString(body);
            if (!el.isJsonObject()) return;
            JsonObject obj = el.getAsJsonObject();

            JsonElement vEl = obj.get("v");
            if (vEl == null || !vEl.isJsonPrimitive()) return;
            int version = vEl.getAsInt();

            // Only update ETag after successful parse so bad responses don't poison the cache
            response.headers().firstValue("ETag").ifPresent(e -> lastEtag = e);

            if (lastKnownVersion == -1) {
                // First check - record the version, trigger refresh if forced (e.g., world change)
                if (force) {
                    if (RemoteDataSync.refreshAllSync()) {
                        lastKnownVersion = version;
                    }
                } else {
                    lastKnownVersion = version;
                }
                return;
            }

            if (version != lastKnownVersion || force) {
                if (RemoteDataSync.refreshAllSync()) {
                    lastKnownVersion = version;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            IslesLog.runtimeInfo("[Isles+] RefreshPoller: check failed: " + e.getMessage());
        }
    }
}
