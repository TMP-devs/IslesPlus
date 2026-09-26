package com.islesplus.sync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnouncementsTest {
    private KeyPair kp;

    @BeforeEach void setUp() throws Exception {
        AnnouncementFeed.resetForTest();
        Announcements.lastShownId = "";
        Announcements.lastShownIssued = "";
        kp = AnnouncementEnvelopeTest.keys();
    }

    private void feed(String id, String issued) throws Exception {
        String p = "{\"id\":\"" + id + "\",\"issued\":\"" + issued + "\",\"text\":\"hi\"}";
        AnnouncementFeed.offer(AnnouncementEnvelopeTest.envelope(p, AnnouncementEnvelopeTest.sign(kp, p)),
            kp.getPublic(), AnnouncementFeedTest.NOW);
    }

    @Test void pendingIsTheFeedsAnnouncement() throws Exception {
        feed("a1", "2026-09-25T08:00:00Z");
        assertEquals("a1", Announcements.pending().id());
    }

    @Test void nothingPendingOnceShown() throws Exception {
        feed("a1", "2026-09-25T08:00:00Z");
        Announcements.markShown(Announcements.pending());
        assertNull(Announcements.pending());
    }

    @Test void markShownRemembersWhenItWasIssued() throws Exception {
        feed("a1", "2026-09-25T08:00:00Z");
        Announcements.markShown(Announcements.pending());
        assertEquals("2026-09-25T08:00:00Z", Announcements.lastShownIssued);
    }

    /** B was shown, the game restarts, and a stale cache serves the older A: A must not show */
    @Test void anOlderCopyAfterARestartIsNotShown() throws Exception {
        Announcements.lastShownId = "b";
        Announcements.lastShownIssued = "2026-09-25T09:00:00Z";
        Announcements.restoreFeed();
        feed("a", "2026-09-25T08:00:00Z");
        assertNull(Announcements.pending());
        feed("b", "2026-09-25T09:00:00Z");
        assertNull(Announcements.pending());
    }

    @Test void aBadSavedIssuedTimeIsIgnored() throws Exception {
        Announcements.lastShownIssued = "not a time";
        Announcements.restoreFeed();
        feed("a1", "2026-09-25T08:00:00Z");
        assertEquals("a1", Announcements.pending().id());
    }
}
