package com.islesplus.sync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncementEnvelopeTest {
    /** Emoji and an accent, so the signed bytes are not plain ASCII. */
    static final String TEXT = "Berry Alert is back 🍓 für alle";
    static final String PAYLOAD = "{\"id\":\"2026-09-25T08-15-00-a1b2\",\"issued\":\"2026-09-25T08:15:00Z\","
        + "\"text\":\"" + TEXT + "\",\"link\":\"\",\"link_text\":\"\",\"color\":\"\"}";

    static KeyPair keys() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    static String sign(KeyPair kp, String payload) throws Exception {
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(kp.getPrivate());
        s.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    static String envelope(String payload, String sig) {
        JsonObject o = new JsonObject();
        o.addProperty("payload", payload);
        o.addProperty("sig", sig);
        return o.toString();
    }

    @Test void opensAGoodEnvelope() throws Exception {
        KeyPair kp = keys();
        AnnouncementEnvelope.Opened o = AnnouncementEnvelope.open(envelope(PAYLOAD, sign(kp, PAYLOAD)), kp.getPublic());
        assertNotNull(o);
        assertEquals("2026-09-25T08-15-00-a1b2", o.announcement().id());
        assertEquals(TEXT, o.announcement().text());
        assertEquals(Instant.parse("2026-09-25T08:15:00Z"), o.issued());
    }

    @Test void editedPayloadIsRejected() throws Exception {
        KeyPair kp = keys();
        String sig = sign(kp, PAYLOAD);
        assertNull(AnnouncementEnvelope.open(envelope(PAYLOAD.replace("back", "gone"), sig), kp.getPublic()));
    }

    @Test void otherKeyIsRejected() throws Exception {
        assertNull(AnnouncementEnvelope.open(envelope(PAYLOAD, sign(keys(), PAYLOAD)), keys().getPublic()));
    }

    @Test void malformedBodiesAreRejected() throws Exception {
        KeyPair kp = keys();
        assertNull(AnnouncementEnvelope.open(null, kp.getPublic()));
        assertNull(AnnouncementEnvelope.open("", kp.getPublic()));
        assertNull(AnnouncementEnvelope.open("not json", kp.getPublic()));
        assertNull(AnnouncementEnvelope.open("[]", kp.getPublic()));
        assertNull(AnnouncementEnvelope.open("{}", kp.getPublic()));
        assertNull(AnnouncementEnvelope.open("{\"payload\":\"x\"}", kp.getPublic()));
        assertNull(AnnouncementEnvelope.open(envelope(PAYLOAD, "not base64 !!"), kp.getPublic()));
        assertNull(AnnouncementEnvelope.open(envelope("not json", sign(kp, "not json")), kp.getPublic()));
        assertNull(AnnouncementEnvelope.open(envelope(PAYLOAD, sign(kp, PAYLOAD)), null));
    }

    @Test void payloadWithoutIssuedIsRejected() throws Exception {
        KeyPair kp = keys();
        String p = "{\"id\":\"a1\",\"text\":\"hi\"}";
        assertNull(AnnouncementEnvelope.open(envelope(p, sign(kp, p)), kp.getPublic()));
    }

    @Test void payloadWithBlankTextIsRejected() throws Exception {
        KeyPair kp = keys();
        String p = "{\"id\":\"a1\",\"issued\":\"2026-09-25T08:15:00Z\",\"text\":\"  \"}";
        assertNull(AnnouncementEnvelope.open(envelope(p, sign(kp, p)), kp.getPublic()));
    }

    @Test void aSignedClearOpensAsCleared() throws Exception {
        KeyPair kp = keys();
        String p = "{\"id\":\"c1\",\"issued\":\"2026-09-25T09:00:00Z\",\"cleared\":true,\"text\":\"\"}";
        AnnouncementEnvelope.Opened o = AnnouncementEnvelope.open(envelope(p, sign(kp, p)), kp.getPublic());
        assertTrue(o.cleared());
        assertNull(o.announcement());
        assertEquals(java.time.Instant.parse("2026-09-25T09:00:00Z"), o.issued());
    }

    @Test void aClearNeedsTheSignatureAndATime() throws Exception {
        KeyPair kp = keys();
        String p = "{\"id\":\"c1\",\"issued\":\"2026-09-25T09:00:00Z\",\"cleared\":true}";
        assertNull(AnnouncementEnvelope.open(envelope(p, sign(keys(), p)), kp.getPublic()));
        String noTime = "{\"id\":\"c1\",\"cleared\":true}";
        assertNull(AnnouncementEnvelope.open(envelope(noTime, sign(kp, noTime)), kp.getPublic()));
        String notBool = "{\"id\":\"c1\",\"issued\":\"2026-09-25T09:00:00Z\",\"cleared\":\"yes\",\"text\":\"\"}";
        assertNull(AnnouncementEnvelope.open(envelope(notBool, sign(kp, notBool)), kp.getPublic()));
    }

    /** Signed by the same bytes the Worker produces (announce-worker/test/announcement.test.ts checks
     * the Worker signs this payload to exactly this signature). */
    @Test void acceptsTheWorkersCrossLanguageFixture() throws Exception {
        Path fixture = Path.of("announce-worker/test/fixtures/cross-language.json");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(fixture), "no announce-worker in this checkout");
        JsonObject f = JsonParser.parseString(Files.readString(fixture, StandardCharsets.UTF_8)).getAsJsonObject();
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
            new X509EncodedKeySpec(Base64.getDecoder().decode(f.get("publicKeyB64").getAsString())));
        AnnouncementEnvelope.Opened o = AnnouncementEnvelope.open(
            envelope(f.get("payload").getAsString(), f.get("sig").getAsString()), key);
        assertNotNull(o);
        assertEquals("2026-09-25T08-15-00-a1b2", o.announcement().id());
        assertEquals("details", o.announcement().linkText());
    }
}
