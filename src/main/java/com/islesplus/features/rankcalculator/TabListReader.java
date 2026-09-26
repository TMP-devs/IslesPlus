package com.islesplus.features.rankcalculator;

import com.islesplus.mixin.PlayerListHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads every piece of text the tab list shows: header, footer and each entry's display name, the
 * entries in the order the tab draws them. Rows read as pairs ("Active Dish: ..." then "4 mins
 * remaining") rely on that: the handler's own collection is a HashMap, and a player or NPC coming
 * into range reshuffled it, so the row after "Active Dish" was often something else entirely.
 * Vanilla's order, but not its 80-row cap: Isles lists more than that.
 */
public final class TabListReader {
    private TabListReader() {}

    public static List<String> lines(MinecraftClient client) {
        List<String> lines = new ArrayList<>();
        PlayerListHudAccessor hud = (PlayerListHudAccessor) client.inGameHud.getPlayerListHud();
        add(lines, hud.getHeader());
        add(lines, hud.getFooter());
        if (client.getNetworkHandler() != null) {
            List<PlayerListEntry> entries = new ArrayList<>(client.getNetworkHandler().getListedPlayerListEntries());
            entries.sort(PlayerListHudAccessor.getEntryOrdering());
            for (PlayerListEntry entry : entries) {
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
