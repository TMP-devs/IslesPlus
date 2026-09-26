package com.islesplus.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * announcement.json, as the /announce Worker writes it: {"payload": "<json text>", "sig": "<base64>"}.
 * Payload and signature travel in one file so a cached copy can never pair a new message with an old
 * signature. The signature (SHA256withRSA) covers the payload string's UTF-8 bytes exactly.
 */
public final class AnnouncementEnvelope {
    /** A verified announcement and when it was issued. {@code announcement} is null for a signed
     * "cleared" payload: from {@code issued} on there is nothing to show. */
    public record Opened(Announcement announcement, Instant issued) {
        public boolean cleared() { return announcement == null; }
    }

    private AnnouncementEnvelope() {}

    /** The announcement in {@code body}, or null when the body is malformed, the signature does not
     * check out with {@code key}, or the payload has no id, text or issued time. A signed payload
     * with {@code "cleared": true} and an issued time opens as {@link Opened#cleared()}. */
    public static Opened open(String body, PublicKey key) {
        if (body == null || key == null) return null;
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) return null;
            String payload = str(root.getAsJsonObject(), "payload");
            String sig = str(root.getAsJsonObject(), "sig");
            if (payload == null || sig == null || !verify(payload, sig, key)) return null;
            JsonElement p = JsonParser.parseString(payload);
            if (!p.isJsonObject()) return null;
            Instant issued = issued(p.getAsJsonObject());
            JsonElement cleared = p.getAsJsonObject().get("cleared");
            if (cleared != null && cleared.isJsonPrimitive() && cleared.getAsJsonPrimitive().isBoolean() && cleared.getAsBoolean()) {
                return issued == null ? null : new Opened(null, issued);
            }
            Announcement a = Announcement.parse(p);
            return a == null || issued == null ? null : new Opened(a, issued);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static boolean verify(String payload, String sigB64, PublicKey key) {
        try {
            Signature v = Signature.getInstance("SHA256withRSA");
            v.initVerify(key);
            v.update(payload.getBytes(StandardCharsets.UTF_8));
            return v.verify(Base64.getDecoder().decode(sigB64.trim()));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    private static Instant issued(JsonObject p) {
        String s = str(p, "issued");
        if (s == null) return null;
        try {
            return Instant.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }
}
