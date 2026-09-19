package com.islesplus.features.rankcalculator;

import com.islesplus.mixin.PlayerListHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Reads every piece of text the tab list shows: header, footer and each entry's display name. */
final class TabListReader {
    private TabListReader() {}

    static List<String> lines(MinecraftClient client) {
        List<String> lines = new ArrayList<>();
        PlayerListHudAccessor hud = (PlayerListHudAccessor) client.inGameHud.getPlayerListHud();
        add(lines, hud.getHeader());
        add(lines, hud.getFooter());
        if (client.getNetworkHandler() != null) {
            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                add(lines, entry.getDisplayName());
            }
        }
        return lines;
    }

    private static void add(List<String> lines, Text text) {
        if (text == null) return;
        // Headers and footers are multi-line; split so each tab row is matched on its own.
        for (String line : text.getString().split("\n")) lines.add(line);
    }
}
