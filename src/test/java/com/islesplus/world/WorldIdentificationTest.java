package com.islesplus.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sidebar lines are the real ones logged in play on 2026-09-20. */
class WorldIdentificationTest {
    private static final List<String> HUB = List.of("20 Sep '26 - 06:12:28", "Hub01   v1.0.0");
    private static final List<String> ISLE_02 = List.of("20 Sep '26 - 06:13:13", "Isles02   v1.0.0");
    private static final List<String> ISLE_P02 = List.of("20 Sep '26 - 04:30:40", "IslesP02  v1.0.0");

    @Test
    void connectedAddressSaysWeAreOnTheNetwork() {
        assertTrue(WorldIdentification.isIslesNetwork("play.skyblockisles.net", ""));
        assertTrue(WorldIdentification.isIslesNetwork("play.skyblockisles.net:25565", null));
        assertTrue(WorldIdentification.isIslesNetwork("PLAY.SkyblockIsles.NET", "anything"));
    }

    @Test
    void sidebarTitleIsTheSecondOpinion() {
        assertTrue(WorldIdentification.isIslesNetwork("203.0.113.7:25565", "play.SKYBLOCK ISLES.net"));
        assertTrue(WorldIdentification.isIslesNetwork("", "§6play.§lSKYBLOCK ISLES§r.net"));
    }

    @Test
    void otherServersAreNotTheNetwork() {
        assertFalse(WorldIdentification.isIslesNetwork("mc.hypixel.net", "HYPIXEL"));
        assertFalse(WorldIdentification.isIslesNetwork(null, null));
    }

    @Test
    void serverNameComesOffTheServerLine() {
        assertEquals("Hub01", WorldIdentification.serverNameOf(HUB));
        assertEquals("Isles02", WorldIdentification.serverNameOf(ISLE_02));
        assertEquals("IslesP02", WorldIdentification.serverNameOf(ISLE_P02));
        assertEquals("Dungeon04", WorldIdentification.serverNameOf(List.of("Goblin Grottos", "Dungeon04  1.0.0")));
        assertNull(WorldIdentification.serverNameOf(List.of("20 Sep '26 - 06:12:28", "Rift Time: 9m 23s (149/287)")));
    }

    /** Plushie coordinates showed in the hub when everything on the network counted as an Isle. */
    @Test
    void theHubIsNotAnIsle() {
        assertEquals(Boolean.FALSE, WorldIdentification.isleVerdict(HUB, true, false));
        assertEquals(Boolean.FALSE, WorldIdentification.isleVerdict(HUB, true, true));
        assertEquals(Boolean.FALSE, WorldIdentification.isleVerdict(List.of("Lobby-3  v1.0.0"), true, true));
    }

    /** IslesP02 was read as "not Isles", which switched off every Isles-only feature there. */
    @Test
    void anyOtherServerOnTheNetworkIsAnIsle() {
        assertEquals(Boolean.TRUE, WorldIdentification.isleVerdict(ISLE_02, true, false));
        assertEquals(Boolean.TRUE, WorldIdentification.isleVerdict(ISLE_P02, true, false));
        assertEquals(Boolean.TRUE, WorldIdentification.isleVerdict(List.of("Some-New-Server-7  v2.0.0"), true, false));
    }

    @Test
    void aSidebarStillFillingInWaitsUnlessLenient() {
        List<String> noServerLineYet = List.of("20 Sep '26 - 06:13:13");
        assertNull(WorldIdentification.isleVerdict(List.of(), true, true));
        assertNull(WorldIdentification.isleVerdict(noServerLineYet, true, false));
        assertEquals(Boolean.TRUE, WorldIdentification.isleVerdict(noServerLineYet, true, true));
    }

    @Test
    void offTheNetworkOnlyTheOldNamePatternCounts() {
        assertEquals(Boolean.TRUE, WorldIdentification.isleVerdict(ISLE_02, false, false));
        assertEquals(Boolean.TRUE, WorldIdentification.isleVerdict(ISLE_P02, false, false));
        assertNull(WorldIdentification.isleVerdict(List.of("Survival  v3.2.1"), false, true));
        assertNull(WorldIdentification.isleVerdict(List.of("play.SKYBLOCKISLES.net"), false, true));
    }

    @Test
    void partyRowsAreNeverRead() {
        // The only sidebar text a player picks. A member called "DungeonGuy" would otherwise read
        // as a rift, "IslesFan01" as a server.
        assertTrue(WorldIdentification.isPartyLine("P » CreatorWodash"));
        assertTrue(WorldIdentification.isPartyLine("P » [unknown player head] chrrisk ❤126"));
        assertTrue(WorldIdentification.isPartyLine("P » DungeonGuy"));
        assertTrue(WorldIdentification.isPartyLine("P » IslesFan01"));
    }

    @Test
    void serverLinesAreNotMistakenForPartyRows() {
        assertFalse(WorldIdentification.isPartyLine("IslesP02  v1.0.0"));
        assertFalse(WorldIdentification.isPartyLine("IslesG02   v1.0.0"));
        assertFalse(WorldIdentification.isPartyLine("Hub01   v1.0.0"));
        assertFalse(WorldIdentification.isPartyLine("DungeonsG04  v1.0.0"));
        assertFalse(WorldIdentification.isPartyLine("20 Sep '26 - 06:12:28"));
    }

    @Test
    void aTrailingBareSectionSignIsStripped() {
        // Every row on the live sidebar ends in one. While it survived, no server line ever parsed.
        assertEquals("IslesG02   v1.0.0", WorldIdentification.stripColor("IslesG02   v1.0.0§"));
        assertEquals("Hub01   v1.0.0", WorldIdentification.stripColor("Hub01   v1.0.0§"));
        assertEquals("IslesG02   v1.0.0", WorldIdentification.stripColor("§aIslesG02   v1.0.0§"));
        assertEquals("", WorldIdentification.stripColor("§"));
    }

    @Test
    void theLiveSidebarParsesEndToEnd() {
        List<String> live = List.of(
            WorldIdentification.stripColor("20 Sep '26 - 10:41:19§"),
            WorldIdentification.stripColor("IslesG02   v1.0.0§"));
        assertEquals("IslesG02", WorldIdentification.serverNameOf(live));
        assertFalse(WorldIdentification.isRift(live));
        assertEquals(Boolean.TRUE, WorldIdentification.isleVerdict(live, true, false));
    }

    @Test
    void theHubIsRuledOutOnTheLiveFormat() {
        List<String> live = List.of(
            WorldIdentification.stripColor("20 Sep '26 - 10:41:19§"),
            WorldIdentification.stripColor("Hub01   v1.0.0§"));
        assertEquals(Boolean.FALSE, WorldIdentification.isleVerdict(live, true, true));
    }

    @Test
    void theServerLineDecidesTheRift() {
        assertTrue(WorldIdentification.isRift(List.of("Goblin Grottos", "Dungeon04  v1.0.0")));
        assertTrue(WorldIdentification.isRift(List.of("DungeonsG04  v1.0.0")));
        assertFalse(WorldIdentification.isRift(ISLE_02));
        assertFalse(WorldIdentification.isRift(HUB));
    }

    @Test
    void aDungeonWordElsewhereDoesNotMakeItARift() {
        // A quest or stat row on an Isle must not pin the world to RIFT.
        assertFalse(WorldIdentification.isRift(List.of("Dungeons cleared: 3", "IslesG02   v1.0.0")));
        // With no server line to go on, the old any-row reading still applies.
        assertTrue(WorldIdentification.isRift(List.of("Dungeons cleared: 3")));
    }
}
