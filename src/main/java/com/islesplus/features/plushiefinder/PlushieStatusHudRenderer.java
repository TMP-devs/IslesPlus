package com.islesplus.features.plushiefinder;

import com.islesplus.features.rankcalculator.RiftRepository;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

public final class PlushieStatusHudRenderer {
    private static final String TEXT = "Plushie #1 is locked behind the tutorial";
    private static final int PAD_X = 3, PAD_Y = 2;

    /** A one-line note when the nearest plushie you are missing cannot be reached yet. */
    public static final HudElement ELEMENT = new HudElement("plushie_status", "Plushie Note",
        new HudPlacement(HudAnchor.START, HudAnchor.START, 5, 6)) {
        @Override public boolean enabled() { return PlushieFinder.plushieFinderEnabled; }
        @Override public boolean active(MinecraftClient client) {
            boolean inPlushieWorld = WorldIdentification.world == PlayerWorld.ISLE
                || ((WorldIdentification.world == PlayerWorld.RIFT || WorldIdentification.world == PlayerWorld.DISABLED_RIFT)
                    && RiftRepository.getPlushieRifts().contains(WorldIdentification.currentRiftName));
            if (!PlushieFinder.plushieFinderEnabled || !inPlushieWorld || FeatureFlags.isKilled("plushie_finder")) return false;
            if (client.player == null || client.world == null || client.textRenderer == null) return false;
            if (PlushieRepository.getCachedPlushies().isEmpty()) return false;

            Vec3d playerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            PlushieEntry tracked = PlushieRepository.getClosestUnowned(playerPos);
            return tracked != null && tracked.num == 1;
        }
        @Override public Size measure(boolean preview) {
            MinecraftClient client = MinecraftClient.getInstance();
            return new Size(client.textRenderer.getWidth(TEXT) + 2 * PAD_X, client.textRenderer.fontHeight + 2 * PAD_Y);
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            MinecraftClient client = MinecraftClient.getInstance();
            Size s = measure(f.preview());
            ctx.fill(0, 0, s.w(), s.h(), 0xBB141414);
            ctx.drawText(client.textRenderer, TEXT, PAD_X, PAD_Y, 0xFFFFA0A0, true);
        }
    };

    private PlushieStatusHudRenderer() {}
}
