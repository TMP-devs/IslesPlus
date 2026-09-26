package com.islesplus.sync;

import com.islesplus.logging.IslesLog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * The announcement sent with /announce in the Isles+ Discord. The Worker writes announcement.json to
 * the islesplusjson repo; this downloads it from raw.githubusercontent.com (cached there for up to
 * 5 minutes) on the refresh poller's loop, checks it was signed with the announcement key, and keeps
 * the newest one. {@link Announcements} decides whether it still needs showing.
 */
public final class AnnouncementFeed {
    static final String URL = "https://raw.githubusercontent.com/TMP-devs/islesplusjson/main/announcement.json";

    /** RSA-2048 public key (X.509 DER, base64) of the /announce Worker. Its own key: not the
     * people.json / features_v2.json one in {@link SignedFlags}. */
    static final String PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApHNUUc4jMEfsbpbLpvL5" +
        "cagWC1VX4ga/+FUwzybse1xsXedDXWrSGIK43hvqgmLr1T8iM+nUJLCq3TZfCWE/" +
        "Pr4XVN5xIVtgSDx8wbKod3AUJREoH/lk29ieMPP354X+D0ubMDrHxeOEGEWEefuB" +
        "CeetL4iHsxVZ1Dp648i/Vdz1j9qQdjlVwH653SPUnBTE259ysJXH9R8FDQsZJSvb" +
        "BDCPktb2kJfuv6uRfvi963ASylbLPvm7b7fReta2ATelSBs6U8nDAKapqNKOVuN4" +
        "e4qU0FTT/8nwQHlozclva3BoZU+QjW4oeqrhJKfVD0zVG0BhZbxRV9rKxJlV6cxs" +
        "8wIDAQAB";

    /** Announcements older than this are never shown: they explain something that may no longer be
     * true, and a player who installs or comes back later should not get old news. */
    static final Duration MAX_AGE = Duration.ofDays(3);

    enum Offer { ACCEPTED, NOT_NEWER, TOO_OLD, INVALID }

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private static volatile AnnouncementEnvelope.Opened latest;
    private static volatile Instant newestIssued = Instant.EPOCH;
    private static volatile String etag = "";
    private static boolean warnedInvalid;

    private AnnouncementFeed() {}

    /** The newest verified announcement, or null when there has been none or the newest thing
     * signed was a clear. */
    public static Announcement current() {
        AnnouncementEnvelope.Opened o = latest;
        return o == null ? null : o.announcement();
    }

    /** The newest verified announcement with its issued time, or null. */
    static AnnouncementEnvelope.Opened latest() { return latest; }

    /** Anything issued at or before {@code issued} counts as already seen: used on start-up with the
     * issued time of the last announcement shown, so a stale cached copy cannot show again. */
    public static synchronized void markSeen(Instant issued) {
        if (issued != null && issued.isAfter(newestIssued)) newestIssued = issued;
    }

    /** Takes a downloaded announcement.json and keeps it when it is valid and newer than the one held. */
    static synchronized Offer offer(String body, PublicKey key, Instant now) {
        AnnouncementEnvelope.Opened o = AnnouncementEnvelope.open(body, key);
        if (o == null) return Offer.INVALID;
        if (o.issued().isBefore(now.minus(MAX_AGE))) return Offer.TOO_OLD;
        if (!o.issued().isAfter(newestIssued)) return Offer.NOT_NEWER;
        latest = o;
        newestIssued = o.issued();
        return Offer.ACCEPTED;
    }

    /** Key in features_v2.json that switches announcements off ("disabled" or "killed"). Json only:
     * there is no card for it in /ip. */
    public static final String FLAG = "announcements";

    /** Downloads announcement.json once, unless announcements are switched off in features_v2.json.
     * Blocking: call from a background thread. */
    public static void refresh() {
        if (FeatureFlags.isKilled(FLAG)) return;
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder().uri(URI.create(URL)).timeout(Duration.ofSeconds(8)).GET();
            String tag = etag;
            if (!tag.isEmpty()) req.header("If-None-Match", tag);
            HttpResponse<String> res = HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 304 || res.statusCode() < 200 || res.statusCode() >= 300) return;
            Offer result = offer(res.body(), publicKey(), Instant.now());
            if (result == Offer.INVALID) {
                if (!warnedInvalid) IslesLog.runtimeWarn("[Isles+] AnnouncementFeed: announcement.json failed the signature check, ignoring it");
                warnedInvalid = true;
                return;
            }
            warnedInvalid = false;
            res.headers().firstValue("ETag").ifPresent(e -> etag = e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            IslesLog.runtimeInfo("[Isles+] AnnouncementFeed: fetch failed: " + e.getMessage());
        }
    }

    private static PublicKey cachedKey;

    static synchronized PublicKey publicKey() {
        if (cachedKey != null) return cachedKey;
        try {
            byte[] der = Base64.getDecoder().decode(PUBLIC_KEY_B64);
            cachedKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            cachedKey = null;
        }
        return cachedKey;
    }

    static synchronized void resetForTest() {
        latest = null;
        newestIssued = Instant.EPOCH;
        etag = "";
        warnedInvalid = false;
    }
}
