package com.islesplus.features.playerfinder;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.ui.GlowColor;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public final class PlayerFinder {
    public static boolean playerFinderEnabled = true;
    public static float glowHue = 0.333f; // green
    public static float glowSaturation = 1.0f;
    public static float glowLightness = 0.5f;
    private static volatile Set<Integer> glowingEntityIds = Set.of();

    private PlayerFinder() {
    }

    public static int glowRgb() {
        return GlowColor.rgb(glowHue, glowSaturation, glowLightness);
    }

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!playerFinderEnabled || WorldIdentification.world != PlayerWorld.RIFT || client.player == null || client.world == null) {
            glowingEntityIds = Set.of();
            return;
        }

        Set<Integer> nextGlowEntityIds = new HashSet<>();
        for (Entity player : scan.players) {
            nextGlowEntityIds.add(player.getId());
        }

        glowingEntityIds = Set.copyOf(nextGlowEntityIds);
    }

    public static void reset() {
        glowingEntityIds = Set.of();
    }

    public static boolean shouldForceGlow(Entity entity) {
        return playerFinderEnabled && entity != null && glowingEntityIds.contains(entity.getId());
    }
}
