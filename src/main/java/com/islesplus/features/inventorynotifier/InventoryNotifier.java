package com.islesplus.features.inventorynotifier;

import com.islesplus.sound.ModSounds;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.sound.SoundConfig;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
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
                confirmKeyName = confirmKeyText.getString();   // both lines are drawn by renderHud()
                lastInventoryFullNotifyMs = now;
            }
        }
    }

    private static final float TITLE_SCALE = 4f, SUBTITLE_SCALE = 2f;
    private static String confirmKeyName = "";

    public static void renderHud(DrawContext ctx, MinecraftClient client) {
        if (!showing || client.options.hudHidden) return;
        // tick() stops running once the feature is remotely disabled, so it cannot clear "showing".
        if (!inventoryFullNotifyEnabled || FeatureFlags.isKilled("inventory_full")) { showing = false; return; }
        drawTitle(ctx, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), confirmKeyName);
    }

    /** Both lines where a vanilla title and subtitle sit (4x and 2x, centred), each with the
     * Isles+ drop shadow ({@link Fonts#drawShadowed}). Drawn by us rather than as a vanilla title
     * because the game's own shadow goes down and right, which leaves gaps inside Silkscreen's
     * letters. */
    public static void drawTitle(DrawContext ctx, int screenW, int screenH, String keyName) {
        String title = "Inventory Full!";
        String subtitle = "Press " + keyName + " to confirm";
        Fonts.drawShadowed(ctx, title, (screenW - Fonts.width(title, TITLE_SCALE)) / 2, screenH / 2 - 40, Theme.HUD_ALERT, TITLE_SCALE);
        Fonts.drawShadowed(ctx, subtitle, (screenW - Fonts.width(subtitle, SUBTITLE_SCALE)) / 2, screenH / 2 + 10, Theme.HUD_TEXT, SUBTITLE_SCALE);
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
