package com.islesplus.features.playerfinder;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public final class PlayerFinder {
    public static boolean playerFinderEnabled = false;
    public static float glowHue = 0.333f; // green
    private static volatile Set<Integer> glowingEntityIds = Set.of();

    private PlayerFinder() {
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
