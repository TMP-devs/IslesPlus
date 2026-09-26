package com.islesplus.features.inventorynotifier;

import com.islesplus.IslesClient;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
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
                confirmKeyName = confirmKeyText.getString();   // both lines are drawn by ELEMENT
                lastInventoryFullNotifyMs = now;
            }
        }
    }

    private static final float TITLE_SCALE = 4f, SUBTITLE_SCALE = 2f;
    /** Subtitle top, below the title's top: where a vanilla title and subtitle sit (H/2-40, H/2+10). */
    private static final int SUBTITLE_Y = 50;
    private static final String TITLE = "Inventory Full!";
    private static String confirmKeyName = "";

    /** Both lines where a vanilla title and subtitle sit by default (4x and 2x, centred), each with
     * the Isles+ drop shadow ({@link Fonts#drawShadowed}). Drawn by us rather than as a vanilla
     * title because the game's own shadow goes down and right, which leaves gaps inside
     * the letters. */
    public static final HudElement ELEMENT = new HudElement("inventory_full", "Inventory Full",
        new HudPlacement(HudAnchor.CENTER, HudAnchor.CENTER, 0, -6)) {
        @Override public boolean enabled() { return inventoryFullNotifyEnabled; }
        @Override public boolean active(MinecraftClient client) {
            if (!showing || client.options.hudHidden) return false;
            // tick() stops running once the feature is remotely disabled, so it cannot clear "showing".
            if (!inventoryFullNotifyEnabled || FeatureFlags.isKilled("inventory_full")) { showing = false; return false; }
            return true;
        }
        @Override public Size measure(boolean preview) {
            int w = Math.max(Fonts.hudWidth(TITLE, TITLE_SCALE), Fonts.hudWidth(subtitle(preview), SUBTITLE_SCALE));
            return new Size(w, SUBTITLE_Y + Fonts.height(SUBTITLE_SCALE) + Math.round(Fonts.snap(SUBTITLE_SCALE)));
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            int w = measure(f.preview()).w();
            String sub = subtitle(f.preview());
            Fonts.drawShadowed(ctx, TITLE, (w - Fonts.hudWidth(TITLE, TITLE_SCALE)) / 2, 0, Theme.HUD_ALERT, TITLE_SCALE);
            Fonts.drawShadowed(ctx, sub, (w - Fonts.hudWidth(sub, SUBTITLE_SCALE)) / 2, SUBTITLE_Y, Theme.HUD_TEXT, SUBTITLE_SCALE);
        }
    };

    private static String subtitle(boolean preview) {
        String key = confirmKeyName;
        if (preview || key.isEmpty()) key = IslesClient.CONFIRM_INVENTORY_FULL_KEY.getBoundKeyLocalizedText().getString();
        return "Press " + key + " to confirm";
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
