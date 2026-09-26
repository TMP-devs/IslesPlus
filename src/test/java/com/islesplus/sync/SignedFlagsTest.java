package com.islesplus.sync;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignedFlagsTest {
    private static final String JSON = "{\n  \"issued\": \"2026-09-25T18:00:00Z\",\n  \"features\": { \"chest_finder\": { \"killed\": true } }\n}\n";

    private static String sign(KeyPair kp, String text) throws Exception {
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(kp.getPrivate());
        s.update(SignedFlags.canonical(text).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    private static KeyPair keys() throws Exception { return KeyPairGenerator.getInstance("RSA").generateKeyPair(); }

    @Test void goodSignaturePasses() throws Exception {
        KeyPair kp = keys();
        assertTrue(SignedFlags.verify(JSON, sign(kp, JSON), kp.getPublic()));
    }

    @Test void anyEditFails() throws Exception {
        KeyPair kp = keys();
        String sig = sign(kp, JSON);
        assertFalse(SignedFlags.verify(JSON.replace("true", "false"), sig, kp.getPublic()));
    }

    @Test void otherKeyFails() throws Exception {
        assertFalse(SignedFlags.verify(JSON, sign(keys(), JSON), keys().getPublic()));
    }

    @Test void lineEndingsAndBomDoNotMatter() throws Exception {
        KeyPair kp = keys();
        String sig = sign(kp, JSON);
        assertTrue(SignedFlags.verify("﻿" + JSON.replace("\n", "\r\n"), sig + "\n", kp.getPublic()));
    }

    @Test void garbageSignatureFails() throws Exception {
        assertFalse(SignedFlags.verify(JSON, "not base64 !!", keys().getPublic()));
        assertFalse(SignedFlags.verify(JSON, null, keys().getPublic()));
    }

    @Test void issuedIsRead() {
        assertEquals(Instant.parse("2026-09-25T18:00:00Z"), SignedFlags.issued(JSON));
        assertEquals(Instant.EPOCH, SignedFlags.issued("{\"features\":{}}"));
        assertEquals(Instant.EPOCH, SignedFlags.issued("{\"issued\":\"yesterday\"}"));
    }

    @Test void builtInKeyLoads() {
        assertNotNull(SignedFlags.publicKey());
    }
}
