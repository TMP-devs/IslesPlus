package com.islesplus.features.inventorynotifier;

import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public class InventoryNotifier {
    private static final long INVENTORY_FULL_REPEAT_MS = 1_400L;

    public static boolean inventoryFullNotifyEnabled = false;
    public static SoundConfig soundConfig = new SoundConfig("minecraft:block.chest.open", 0.62f, 0.95f);

    private static long lastInventoryFullNotifyMs = 0L;
    private static boolean showing = false;
    private static boolean acknowledged = false;

    public static void tick(MinecraftClient client, Text confirmKeyText) {
        if (!inventoryFullNotifyEnabled || client.player == null) {
            showing = false;
            return;
        }
        if (!isInventoryFull(client)) {
            showing = false;
            acknowledged = false;
            return;
        }

        if (!acknowledged) {
            showing = true;
            long now = Util.getMeasuringTimeMs();
            if (now - lastInventoryFullNotifyMs >= INVENTORY_FULL_REPEAT_MS) {
                ModSounds.playConfig(client, soundConfig);
                client.inGameHud.setTitleTicks(0, 40, 5);
                client.inGameHud.setSubtitle(
                    Text.literal("Press " + confirmKeyText.getString() + " to confirm")
                        .formatted(Formatting.WHITE)
                );
                client.inGameHud.setTitle(
                    Text.literal("Inventory Full!").formatted(Formatting.RED)
                );
                lastInventoryFullNotifyMs = now;
            }
        }
    }

    public static void confirm() {
        if (!showing) {
            return;
        }
        acknowledged = true;
        showing = false;
    }

    public static boolean isShowing() {
        return showing;
    }

    private static boolean isInventoryFull(MinecraftClient client) {
        if (client.player == null) {
            return false;
        }
        return client.player.getInventory().getEmptySlot() == -1;
    }

    public static void reset() {
        lastInventoryFullNotifyMs = 0L;
        showing = false;
        acknowledged = false;
    }
}
