package com.islesplus.features.rankcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventAnnouncementTest {
    @Test void readsTheServerBroadcast() {
        assertEquals(ActiveEvent.RIFT_SPELUNKER, ActiveEvent.fromAnnouncement("CURRENT EVENT: Rift Spelunker"));
        assertEquals(ActiveEvent.RIFT_SPELUNKER, ActiveEvent.fromAnnouncement("§a§lCURRENT EVENT: §fRift Spelunker"));
        assertEquals(ActiveEvent.BOSS_BIAS, ActiveEvent.fromAnnouncement("CURRENT EVENT: Boss Bias"));
    }

    @Test void aLeadingBulletOrBracketIsAllowed() {
        assertEquals(ActiveEvent.RIFT_SPELUNKER, ActiveEvent.fromAnnouncement("» CURRENT EVENT: Rift Spelunker"));
        assertEquals(ActiveEvent.RIFT_SPELUNKER, ActiveEvent.fromAnnouncement("§7[§6!§7] §aCurrent Event: Rift Spelunker"));
    }

    @Test void unknownEventIsNoneNotNull() {
        assertEquals(ActiveEvent.NONE, ActiveEvent.fromAnnouncement("CURRENT EVENT: Something New"));
    }

    @Test void ordinaryLinesAreNotAnnouncements() {
        assertNull(ActiveEvent.fromAnnouncement("Rift Grades require 10% less score."));
        assertNull(ActiveEvent.fromAnnouncement("Thomas6767 » current event: rift spelunker lol"));
        assertNull(ActiveEvent.fromAnnouncement("[Party] Bob: CURRENT EVENT: Rift Spelunker"));
        assertNull(ActiveEvent.fromAnnouncement("From Bob: current event: rift spelunker"));
        assertNull(ActiveEvent.fromAnnouncement(null));
    }

    @Test void tabListWinsOverAnnouncement() {
        assertEquals(ActiveEvent.BOSS_BIAS,
            ActiveEvent.resolve(ActiveEvent.BOSS_BIAS, ActiveEvent.RIFT_SPELUNKER, 1_000));
    }

    @Test void recentAnnouncementFillsInWhenTabListIsSilent() {
        assertEquals(ActiveEvent.RIFT_SPELUNKER,
            ActiveEvent.resolve(ActiveEvent.NONE, ActiveEvent.RIFT_SPELUNKER, 5 * 60_000L));
    }

    @Test void staleAnnouncementIsIgnored() {
        assertEquals(ActiveEvent.NONE,
            ActiveEvent.resolve(ActiveEvent.NONE, ActiveEvent.RIFT_SPELUNKER, ActiveEvent.ANNOUNCEMENT_TTL_MS + 1));
    }
}
