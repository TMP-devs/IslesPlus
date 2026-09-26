package com.islesplus.sync;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncementTest {
    private static Announcement parse(String json) {
        return Announcement.parse(JsonParser.parseString(json));
    }

    // --- parsing ---

    @Test void readsEveryField() {
        Announcement a = parse("""
            {
              "id": "2026-09-24T18-22-05Z",
              "text": "Berry Alert is off, the server changed node labels.",
              "link": "https://discord.gg/UKnEWBDJ7w",
              "link_text": "details",
              "color": "#e05341"
            }
            """);
        assertEquals(new Announcement(
            "2026-09-24T18-22-05Z",
            "Berry Alert is off, the server changed node labels.",
            "https://discord.gg/UKnEWBDJ7w",
            "details",
            "#e05341"
        ), a);
    }

    @Test void linkAndColorAreOptional() {
        Announcement a = parse("{\"id\": \"a1\", \"text\": \"Back on.\"}");
        assertEquals(new Announcement("a1", "Back on.", "", "", ""), a);
    }

    @Test void trimsIdAndText() {
        Announcement a = parse("{\"id\": \"  a1  \", \"text\": \"  Back on.  \"}");
        assertEquals("a1", a.id());
        assertEquals("Back on.", a.text());
    }

    @Test void noAnnouncementWhenElementIsMissing() {
        assertNull(Announcement.parse(null));
    }

    @Test void noAnnouncementWhenNotAnObject() {
        assertNull(parse("\"just a string\""));
        assertNull(parse("[]"));
    }

    @Test void noAnnouncementWithoutText() {
        assertNull(parse("{\"id\": \"a1\"}"));
        assertNull(parse("{\"id\": \"a1\", \"text\": \"   \"}"));
    }

    /** without an id we cannot tell it apart from one already shown, so it is not an announcement */
    @Test void noAnnouncementWithoutId() {
        assertNull(parse("{\"text\": \"Back on.\"}"));
        assertNull(parse("{\"id\": \"  \", \"text\": \"Back on.\"}"));
    }

    @Test void emptyObjectClearsTheAnnouncement() {
        assertNull(parse("{}"));
    }

    // --- show once ---

    @Test void unseenWhenNothingShownYet() {
        assertTrue(parse("{\"id\": \"a1\", \"text\": \"hi\"}").isUnseen(""));
    }

    @Test void seenWhenIdMatchesTheLastShown() {
        assertFalse(parse("{\"id\": \"a1\", \"text\": \"hi\"}").isUnseen("a1"));
    }

    @Test void unseenWhenIdChanged() {
        assertTrue(parse("{\"id\": \"a2\", \"text\": \"hi\"}").isUnseen("a1"));
    }

    /** a re-send of the same text under a new id shows again: the id is the whole decision */
    @Test void sameTextNewIdShowsAgain() {
        Announcement first = parse("{\"id\": \"a1\", \"text\": \"hi\"}");
        Announcement again = parse("{\"id\": \"a2\", \"text\": \"hi\"}");
        assertFalse(first.isUnseen("a1"));
        assertTrue(again.isUnseen("a1"));
    }

    @Test void unseenWhenLastShownIsNull() {
        assertTrue(parse("{\"id\": \"a1\", \"text\": \"hi\"}").isUnseen(null));
    }
}
