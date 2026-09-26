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
 * polls a tiny version file on github pages every minute. when the number goes up we
 * call {@link RemoteDataSync#refreshAllSync()} on this background thread to re-pull everything.
 *
 * only polls in {@link PlayerWorld#ISLE} / {@link PlayerWorld#OTHER}, never mid dungeon
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

    /** start polling, called once world detection says we're somewhere safe */
    public static void start() {
        stop();
        Thread t = new Thread(RefreshPoller::pollLoop, "IslesPlus-RefreshPoller");
        t.setDaemon(true);
        pollerThread = t;
        t.start();
    }

    /** stop polling, called on disconnect */
    public static void stop() {
        Thread t = pollerThread;
        if (t != null) {
            t.interrupt();
            pollerThread = null;
        }
    }

    /** force a refresh right now (e.g. just left a dungeon) */
    public static void forceRefresh() {
        Thread checker = new Thread(() -> checkOnce(true), "IslesPlus-RefreshCheck");
        checker.setDaemon(true);
        checker.start();
    }

    private static boolean isSafeWorld(PlayerWorld world) {
        return world == PlayerWorld.ISLE || world == PlayerWorld.OTHER;
    }

    private static void pollLoop() {
        // check once right away (we only get started in safe worlds anyway)
        checkOnce(false);
        AnnouncementFeed.refresh();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (isSafeWorld(WorldIdentification.world)) {
                checkOnce(false);
                AnnouncementFeed.refresh();
            }
        }
    }

    private static synchronized void checkOnce(boolean force) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(8))
                .GET();

            // skip the etag when forced so github gives us the full body
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

            // only save the etag after parsing worked, otherwise a bad response poisons the cache
            response.headers().firstValue("ETag").ifPresent(e -> lastEtag = e);

            if (lastKnownVersion == -1) {
                // first check, just remember the version. refresh too if forced (world change etc)
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
