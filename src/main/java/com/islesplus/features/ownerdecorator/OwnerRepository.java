package com.islesplus.features.ownerdecorator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islesplus.logging.IslesLog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OwnerRepository {
    private static final String JSON_URL = "https://tmp-devs.github.io/islesplusjson/people.json";
    private static final String SIG_URL  = "https://tmp-devs.github.io/islesplusjson/people.json.sig";

    /** RSA-2048 public key, DER-encoded then Base64. Private key lives in GitHub Actions secrets only. */
    private static final String PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoIrh8/O4/OVov507+Ble" +
        "+4DDA+BFkQaRFYJSEYIcUPB2K7dlau1PorFl/rQ1Dhc5674jz6Af5ndJtqq199A/" +
        "Jb4YwXnKaGqzTIBKA9IXd5Mq3MnhSsK8ptr5pJQX8S5K6WyV9+GAjFJxBcG/oc" +
        "hnVF0rB3Sd7xA7ufpasN8BRt2g/GubqHCQXFp8REC2s7icC1HjOVybVRjhHgum" +
        "MD9HmsDtpLuSlMuYgPkz9DGhDaaleyFHd4b/K5nGMvF0FwC0foNkfG/tB0EHZy7" +
        "IOPGTK2c93CTMvCcehNnqShAwpjZE8GxSO3LBtRoGTn6usURLuEaBVltxxifmX3x" +
        "82tmzOQIDAQAB";

    private static final Set<String> FALLBACK_OWNERS = Set.of("chrrisk", "scrolls");

    private static final AtomicBoolean refreshInFlight = new AtomicBoolean(false);
    private static volatile Set<String> owners = FALLBACK_OWNERS;

    private OwnerRepository() {}

    public static void init() {
        refreshRemoteDataNowAsync();
    }

    public static boolean refreshRemoteDataNowAsync() {
        if (!refreshInFlight.compareAndSet(false, true)) return false;
        Thread fetcher = new Thread(() -> {
            try {
                fetchAndVerify();
            } finally {
                refreshInFlight.set(false);
            }
        }, "RemoteDataRefreshOwners");
        fetcher.setDaemon(true);
        fetcher.start();
        return true;
    }

    public static boolean isOwner(String name) {
        return owners.contains(name.toLowerCase(Locale.ROOT));
    }

    public static boolean refreshSync() {
        if (!refreshInFlight.compareAndSet(false, true)) return false;
        try {
            return fetchAndVerify();
        } finally {
            refreshInFlight.set(false);
        }
    }

    public static boolean fetchAndVerify() {
        boolean hasVerifiedData = owners != FALLBACK_OWNERS;
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
            String json = fetch(client, JSON_URL);
            String sig  = fetch(client, SIG_URL);
            if (json == null || sig == null) {
                if (!hasVerifiedData) owners = FALLBACK_OWNERS;
                IslesLog.runtimeInfo("[Isles+] OwnerRepository: fetch failed" + (hasVerifiedData ? ", keeping existing owners" : ", using fallback owners"));
                return false;
            }
            if (!verifySignature(json.getBytes(StandardCharsets.UTF_8), sig.trim())) {
                if (!hasVerifiedData) owners = FALLBACK_OWNERS;
                IslesLog.runtimeWarn("[Isles+] OwnerRepository: SIGNATURE INVALID" + (hasVerifiedData ? ", keeping existing owners" : ", using fallback owners"));
                return false;
            }
            ParsedPeople fetched = parseJson(json);
            if (fetched != null) {
                owners = fetched.owners();
                IslesLog.runtimeInfo("[Isles+] OwnerRepository: loaded " + fetched.owners().size() + " owners (verified)");
                return true;
            } else {
                if (!hasVerifiedData) owners = FALLBACK_OWNERS;
                return false;
            }
        } catch (Exception e) {
            if (!hasVerifiedData) owners = FALLBACK_OWNERS;
            IslesLog.runtimeInfo("[Isles+] OwnerRepository: fetch error: " + e.getMessage() + (hasVerifiedData ? ", keeping existing owners" : ", using fallback owners"));
            return false;
        }
    }

    private static String fetch(HttpClient client, String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) return null;
            return resp.body();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean verifySignature(byte[] data, String sigB64) {
        try {
            byte[] pubKeyBytes = Base64.getDecoder().decode(PUBLIC_KEY_B64.replaceAll("\\s+", ""));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(pubKeyBytes);
            PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(pubKey);
            verifier.update(data);
            byte[] sigBytes = Base64.getDecoder().decode(sigB64);
            return verifier.verify(sigBytes);
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] OwnerRepository: signature check error", e);
            return false;
        }
    }

    private record ParsedPeople(Set<String> owners) {}

    private static ParsedPeople parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return null;
            JsonObject rootObj = root.getAsJsonObject();
            JsonElement ownersElement = rootObj.get("owners");
            if (ownersElement == null || !ownersElement.isJsonArray()) return null;
            Set<String> ownerSet = new HashSet<>();
            for (JsonElement nameElement : ownersElement.getAsJsonArray()) {
                ownerSet.add(nameElement.getAsString().toLowerCase(Locale.ROOT));
            }
            return new ParsedPeople(Collections.unmodifiableSet(ownerSet));
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] OwnerRepository: failed to parse JSON", e);
            return null;
        }
    }
}
