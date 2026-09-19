package com.islesplus.features.chatfilter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatFilterTest {
    @Test void serverDeathBroadcastIsADeathMessage() {
        assertTrue(ChatFilter.isDeathMessage("☠ ➔ SelfSufficiency DIED! ☠ They lost 0 items and 24 coins!"));
    }

    @Test void playerChatMentioningDeathIsNot() {
        assertFalse(ChatFilter.isDeathMessage("Thomas6767 » he DIED! They lost everything lol"));
    }

    @Test void unrelatedLinesAreNot() {
        assertFalse(ChatFilter.isDeathMessage("CURRENT EVENT: Rift Spelunker"));
        assertFalse(ChatFilter.isDeathMessage("They lost the match"));
        assertFalse(ChatFilter.isDeathMessage(null));
    }
}
