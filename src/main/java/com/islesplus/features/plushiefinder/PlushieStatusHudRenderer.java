package com.islesplus.features.plushiefinder;

import com.islesplus.features.rankcalculator.RiftRepository;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

public final class PlushieStatusHudRenderer {
    private PlushieStatusHudRenderer() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        boolean inPlushieWorld = WorldIdentification.world == PlayerWorld.ISLE
            || ((WorldIdentification.world == PlayerWorld.RIFT || WorldIdentification.world == PlayerWorld.DISABLED_RIFT)
                && RiftRepository.getPlushieRifts().contains(WorldIdentification.currentRiftName));
        if (!PlushieFinder.plushieFinderEnabled || !inPlushieWorld || FeatureFlags.isKilled("plushie_finder")) return;
        if (client.player == null || client.world == null || client.textRenderer == null) return;

        if (PlushieRepository.getCachedPlushies().isEmpty()) return;

        Vec3d playerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        PlushieEntry tracked = PlushieRepository.getClosestUnowned(playerPos);
        if (tracked == null || tracked.num != 1) return;

        String text = "Plushie #1 is locked behind the tutorial";
        int x = 8;
        int y = 8;
        int tw = client.textRenderer.getWidth(text);
        ctx.fill(x - 3, y - 2, x + tw + 3, y + client.textRenderer.fontHeight + 2, 0xBB141414);
        ctx.drawText(client.textRenderer, text, x, y, 0xFFFFA0A0, true);
    }

}
