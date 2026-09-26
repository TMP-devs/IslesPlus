package com.islesplus.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnouncementFeedTest {
    static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private KeyPair kp;

    @BeforeEach void setUp() throws Exception {
        AnnouncementFeed.resetForTest();
        kp = AnnouncementEnvelopeTest.keys();
    }

    private String body(String id, String issued, String text) throws Exception {
        String p = "{\"id\":\"" + id + "\",\"issued\":\"" + issued + "\",\"text\":\"" + text + "\"}";
        return AnnouncementEnvelopeTest.envelope(p, AnnouncementEnvelopeTest.sign(kp, p));
    }

    private AnnouncementFeed.Offer offer(String id, String issued) throws Exception {
        return AnnouncementFeed.offer(body(id, issued, "hi"), kp.getPublic(), NOW);
    }

    @Test void nothingBeforeTheFirstOne() {
        assertNull(AnnouncementFeed.current());
    }

    @Test void acceptsTheFirstOne() throws Exception {
        assertEquals(AnnouncementFeed.Offer.ACCEPTED, offer("a1", "2026-09-25T08:00:00Z"));
        assertEquals("a1", AnnouncementFeed.current().id());
    }

    @Test void aNewerOneReplacesIt() throws Exception {
        offer("a1", "2026-09-25T08:00:00Z");
        assertEquals(AnnouncementFeed.Offer.ACCEPTED, offer("a2", "2026-09-25T09:00:00Z"));
        assertEquals("a2", AnnouncementFeed.current().id());
    }

    /** raw.githubusercontent.com can serve an older cached copy after a newer one was seen */
    @Test void anOlderOrTheSameOneIsIgnored() throws Exception {
        offer("a2", "2026-09-25T09:00:00Z");
        assertEquals(AnnouncementFeed.Offer.NOT_NEWER, offer("a1", "2026-09-25T08:00:00Z"));
        assertEquals(AnnouncementFeed.Offer.NOT_NEWER, offer("a2", "2026-09-25T09:00:00Z"));
        assertEquals("a2", AnnouncementFeed.current().id());
    }

    private AnnouncementFeed.Offer clear(String issued) throws Exception {
        String p = "{\"id\":\"c-" + issued + "\",\"issued\":\"" + issued + "\",\"cleared\":true,\"text\":\"\"}";
        return AnnouncementFeed.offer(AnnouncementEnvelopeTest.envelope(p, AnnouncementEnvelopeTest.sign(kp, p)), kp.getPublic(), NOW);
    }

    /** a player who logs in after a clear gets nothing, and a stale cached copy of the old message
     * (older than the clear) cannot bring it back */
    @Test void aClearHidesTheMessageForGood() throws Exception {
        offer("a1", "2026-09-25T08:00:00Z");
        assertEquals(AnnouncementFeed.Offer.ACCEPTED, clear("2026-09-25T09:00:00Z"));
        assertNull(AnnouncementFeed.current());
        assertEquals(AnnouncementFeed.Offer.NOT_NEWER, offer("a1", "2026-09-25T08:00:00Z"));
        assertNull(AnnouncementFeed.current());
    }

    @Test void aNewMessageAfterAClearShows() throws Exception {
        clear("2026-09-25T09:00:00Z");
        assertEquals(AnnouncementFeed.Offer.ACCEPTED, offer("a2", "2026-09-25T09:30:00Z"));
        assertEquals("a2", AnnouncementFeed.current().id());
    }

    @Test void aClearOnAFreshStartShowsNothing() throws Exception {
        assertEquals(AnnouncementFeed.Offer.ACCEPTED, clear("2026-09-25T09:00:00Z"));
        assertNull(AnnouncementFeed.current());
        assertNull(Announcements.pending());
    }

    @Test void anInvalidOneKeepsTheCurrent() throws Exception {
        offer("a1", "2026-09-25T08:00:00Z");
        assertEquals(AnnouncementFeed.Offer.INVALID,
            AnnouncementFeed.offer(body("a2", "2026-09-25T09:00:00Z", "x"), AnnouncementEnvelopeTest.keys().getPublic(), NOW));
        assertEquals(AnnouncementFeed.Offer.INVALID, AnnouncementFeed.offer("garbage", kp.getPublic(), NOW));
        assertEquals("a1", AnnouncementFeed.current().id());
    }

    /** a player who installs or comes back weeks later must not get last month's news */
    @Test void anAnnouncementOlderThanThreeDaysIsIgnored() throws Exception {
        assertEquals(AnnouncementFeed.Offer.TOO_OLD, offer("old", "2026-09-22T09:59:59Z"));
        assertNull(AnnouncementFeed.current());
        assertEquals(AnnouncementFeed.Offer.ACCEPTED, offer("recent", "2026-09-22T10:00:01Z"));
    }

    /** after a restart, the issued time of the last shown one keeps older and repeated copies out */
    @Test void markSeenHidesTheSameAndOlder() throws Exception {
        AnnouncementFeed.markSeen(Instant.parse("2026-09-25T09:00:00Z"));
        assertEquals(AnnouncementFeed.Offer.NOT_NEWER, offer("a2", "2026-09-25T09:00:00Z"));
        assertEquals(AnnouncementFeed.Offer.NOT_NEWER, offer("a1", "2026-09-25T08:00:00Z"));
        assertNull(AnnouncementFeed.current());
        assertEquals(AnnouncementFeed.Offer.ACCEPTED, offer("a3", "2026-09-25T09:30:00Z"));
    }

    /** guards against a bad paste of PUBLIC_KEY_B64 */
    @Test void theBuiltInKeyIsRsa2048() {
        RSAPublicKey k = (RSAPublicKey) AnnouncementFeed.publicKey();
        assertEquals(2048, k.getModulus().bitLength());
    }
}
