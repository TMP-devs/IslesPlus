package com.islesplus.sync;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that a features_v2.json really came from us: it is signed (SHA256withRSA) with the same
 * private key that signs people.json, which only exists as the {@code PEOPLE_SIGN_KEY} secret of
 * the islesplusjson repo; its GitHub Action signs the file on every push. The matching public key
 * is built into the mod. A file or cache that was edited by hand fails the check and is ignored.
 *
 * <p>The signature ({@code features_v2.json.sig}, base64) covers the file's text with any byte
 * order mark and every carriage return removed, so the same file checks out whether git or an
 * editor saved it with Windows or Unix line endings (the Action signs the same cleaned text).
 *
 * <p>The file's top-level {@code "issued"} (ISO time, e.g. "2026-09-25T18:00:00Z") is inside the
 * signed text: the mod never goes back to a file older than one it has already accepted, so an old
 * signed copy (from before a feature was switched off) cannot be fed back in.
 */
public final class SignedFlags {
    /** RSA-2048 public key (X.509 DER, base64): the people.json key, see OwnerRepository. */
    static final String PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoIrh8/O4/OVov507+Ble" +
        "+4DDA+BFkQaRFYJSEYIcUPB2K7dlau1PorFl/rQ1Dhc5674jz6Af5ndJtqq199A/" +
        "Jb4YwXnKaGqzTIBKA9IXd5Mq3MnhSsK8ptr5pJQX8S5K6WyV9+GAjFJxBcG/oc" +
        "hnVF0rB3Sd7xA7ufpasN8BRt2g/GubqHCQXFp8REC2s7icC1HjOVybVRjhHgum" +
        "MD9HmsDtpLuSlMuYgPkz9DGhDaaleyFHd4b/K5nGMvF0FwC0foNkfG/tB0EHZy7" +
        "IOPGTK2c93CTMvCcehNnqShAwpjZE8GxSO3LBtRoGTn6usURLuEaBVltxxifmX3x" +
        "82tmzOQIDAQAB";

    private static final Pattern ISSUED = Pattern.compile("\"issued\"\\s*:\\s*\"([^\"]+)\"");

    private SignedFlags() {}

    /** The text the signature covers: no byte order mark, no carriage returns. */
    public static String canonical(String text) {
        if (text == null) return "";
        String t = text.startsWith("﻿") ? text.substring(1) : text;
        return t.replace("\r", "");
    }

    public static boolean verify(String json, String signatureB64) {
        return verify(json, signatureB64, publicKey());
    }

    static boolean verify(String json, String signatureB64, PublicKey key) {
        if (json == null || signatureB64 == null || key == null) return false;
        try {
            byte[] sig = Base64.getDecoder().decode(signatureB64.trim());
            Signature v = Signature.getInstance("SHA256withRSA");
            v.initVerify(key);
            v.update(canonical(json).getBytes(StandardCharsets.UTF_8));
            return v.verify(sig);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    /** When the file says it was issued; {@link Instant#EPOCH} if it does not say. */
    public static Instant issued(String json) {
        if (json == null) return Instant.EPOCH;
        Matcher m = ISSUED.matcher(json);
        if (!m.find()) return Instant.EPOCH;
        try {
            return Instant.parse(m.group(1).trim());
        } catch (RuntimeException e) {
            return Instant.EPOCH;
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
}
