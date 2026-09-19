package com.islesplus.features.secret;

import com.google.gson.JsonObject;
import com.islesplus.logging.IslesLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tells the Isles+ team that this player found the hidden page, in a way that cannot be faked for
 * somebody else's name:
 * <ol>
 *   <li>the game tells Mojang "I am joining server &lt;random id&gt;" - the very handshake used to
 *       join any online server, done by the game's own session service;</li>
 *   <li>the username and that random id (nothing else - no token, no password) go to our worker;</li>
 *   <li>the worker asks Mojang whether that player really made that handshake, and only then
 *       records the UUID - once, with the time of the first report.</li>
 * </ol>
 * Runs on a background thread, once per game session, and fails silently (offline accounts, no
 * network): it must never get in the way of closing the page.
 */
public final class FoundReport {
    private static final String ENDPOINT = "https://islesplus-found.islesplus-pv-worker.workers.dev/found";
    private static final AtomicBoolean sent = new AtomicBoolean(false);

    private FoundReport() {}

    public static void sendOnce() {
        if (!sent.compareAndSet(false, true)) return;
        Thread t = new Thread(FoundReport::send, "IslesPlus-FoundReport");
        t.setDaemon(true);
        t.start();
    }

    private static void send() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Session session = client.getSession();
            UUID uuid = session.getUuidOrNull();
            if (uuid == null) return;   // offline / dev account: nothing Mojang could confirm

            // Exactly 32 lowercase hex characters. A real Minecraft login hash is a 40-digit SHA-1, so
            // a game server the player joins cannot replay its (name, hash) pair to our worker: the
            // worker only accepts this shape, which a real hash matches about once in 4 billion.
            String serverId = String.format("%032x", new BigInteger(128, new SecureRandom()));
            client.getApiServices().sessionService().joinServer(uuid, session.getAccessToken(), serverId);

            JsonObject body = new JsonObject();
            body.addProperty("username", session.getUsername());
            body.addProperty("serverId", serverId);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
            HttpResponse<String> response;
            try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()) {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            }
            if (response.statusCode() != 200) {
                sent.set(false);   // let a later click try again
                String reply = response.body() == null ? "" : response.body();
                IslesLog.runtimeInfo("[Isles+] found report refused: HTTP " + response.statusCode() + " "
                    + reply.substring(0, Math.min(200, reply.length())));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sent.set(false);
        } catch (Exception e) {
            sent.set(false);
            IslesLog.runtimeInfo("[Isles+] found report not sent: " + e.getMessage());
        }
    }
}
