package com.islesplus.features.quickactions;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickActionTest {
    private static QuickAction parse(String json) {
        return QuickAction.fromJson(JsonParser.parseString(json));
    }

    @Test void commandSlashesAndSpacesAreStripped() {
        assertEquals("warp spawn", QuickAction.normalizeCommand("  /warp spawn "));
        assertEquals("p warp", QuickAction.normalizeCommand("//p warp"));
        assertEquals("", QuickAction.normalizeCommand("/"));
        assertEquals("", QuickAction.normalizeCommand(null));
    }

    @Test void roundTrips() {
        QuickAction a = new QuickAction();
        a.icon = "minecraft:ender_pearl";
        a.type = QuickAction.Type.KEYBIND;
        a.target = "key.togglePerspective";
        QuickAction b = QuickAction.fromJson(a.toJson());
        assertEquals(a.icon, b.icon);
        assertEquals(a.type, b.type);
        assertEquals(a.target, b.target);
    }

    @Test void savedCommandIsNormalizedOnLoad() {
        QuickAction a = parse("{\"type\":\"command\",\"target\":\"/bp\"}");
        assertEquals(QuickAction.Type.COMMAND, a.type);
        assertEquals("bp", a.target);
        assertTrue(a.isSet());
    }

    @Test void junkLoadsAsAnEmptyButton() {
        for (String json : new String[] {"null", "3", "[]", "{}", "{\"type\":\"launch_rockets\",\"target\":\"x\"}"}) {
            QuickAction a = parse(json);
            assertEquals(QuickAction.Type.NONE, a.type, json);
            assertEquals("", a.target, json);
            assertFalse(a.isSet(), json);
        }
    }

    @Test void aTypeWithoutATargetIsNotSet() {
        assertFalse(parse("{\"type\":\"keybind\",\"target\":\"\"}").isSet());
        assertFalse(parse("{\"type\":\"command\",\"target\":\"/\"}").isSet());
    }

    @Test void clearEmptiesEverything() {
        QuickAction a = parse("{\"icon\":\"minecraft:cake\",\"type\":\"isles\",\"target\":\"auto_party\"}");
        a.clear();
        assertEquals("", a.icon);
        assertEquals(QuickAction.Type.NONE, a.type);
        assertFalse(a.isSet());
    }
}
