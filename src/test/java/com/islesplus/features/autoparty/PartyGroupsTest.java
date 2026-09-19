package com.islesplus.features.autoparty;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyGroupsTest {
    List<AutoParty.PartyGroup> g = new ArrayList<>();

    @Test void ensureSeedsFromLegacyFriends() {
        assertEquals(0, PartyGroups.ensureGroup(g, List.of("a", "b"), -1));
        assertEquals(List.of("a", "b"), g.get(0).members);
        assertEquals("Group 1", g.get(0).name);
    }

    @Test void addNamesSequentially() {
        PartyGroups.ensureGroup(g, List.of(), -1);
        assertEquals(1, PartyGroups.addGroup(g));
        assertEquals("Group 2", g.get(1).name);
    }

    @Test void deleteLastResets() {
        PartyGroups.ensureGroup(g, List.of("a"), -1);
        assertEquals(0, PartyGroups.deleteGroup(g, 0));
        assertEquals(1, g.size());
        assertTrue(g.get(0).members.isEmpty());
    }

    @Test void deleteClampsIndex() {
        PartyGroups.ensureGroup(g, List.of(), -1);
        PartyGroups.addGroup(g);
        assertEquals(0, PartyGroups.deleteGroup(g, 1));
    }

    @Test void renameTrimsAndIgnoresBlank() {
        PartyGroups.ensureGroup(g, List.of(), -1);
        PartyGroups.rename(g, 0, "  Delve Crew ");
        assertEquals("Delve Crew", g.get(0).name);
        PartyGroups.rename(g, 0, "   ");
        assertEquals("Delve Crew", g.get(0).name);
    }

    @Test void memberCountPlural() {
        var p = new AutoParty.PartyGroup("x");
        assertEquals("0 members", PartyGroups.memberCount(p));
        p.members.add("a");
        assertEquals("1 member", PartyGroups.memberCount(p));
    }
}
