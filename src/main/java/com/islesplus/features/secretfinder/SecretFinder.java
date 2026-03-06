package com.islesplus.features.secretfinder;

import com.islesplus.IslesPlusConfig;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import com.islesplus.entity.EntityScanResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SecretFinder {
    private static final double POSITION_EPSILON = 0.01;

    public static boolean secretFinderEnabled = false;
    public static float glowHue = 0.917f; // hot pink
    private static volatile Set<Integer> glowingEntityIds = Set.of();
    private static Set<BlockPos> slimeBlockPositions = Set.of();

    private SecretFinder() {}

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!secretFinderEnabled
                || WorldIdentification.world != PlayerWorld.RIFT
                || client.player == null
                || client.world == null) {
            glowingEntityIds = Set.of();
            slimeBlockPositions = Set.of();
            return;
        }

        Set<Integer> next = new HashSet<>();
        for (Entity interaction : scan.interactions) {
            for (Entity display : scan.itemDisplays) {
                if (!positionsMatch(interaction, display)) continue;
                next.add(interaction.getId());
                next.add(display.getId());
                if (next.size() >= IslesPlusConfig.maxMatches) break;
            }
            if (next.size() >= IslesPlusConfig.maxMatches) break;
        }

        Set<BlockPos> nextSlimePositions = new HashSet<>();
        for (Entity slime : scan.slimes) {
            nextSlimePositions.add(new BlockPos(
                MathHelper.floor(slime.getX()),
                MathHelper.floor(slime.getY()),
                MathHelper.floor(slime.getZ())
            ));
        }
        slimeBlockPositions = Collections.unmodifiableSet(nextSlimePositions);

        glowingEntityIds = Set.copyOf(next);
    }

    private static boolean positionsMatch(Entity a, Entity b) {
        return Math.abs(a.getX() - b.getX()) <= POSITION_EPSILON
            && Math.abs(a.getY() - b.getY()) <= POSITION_EPSILON
            && Math.abs(a.getZ() - b.getZ()) <= POSITION_EPSILON;
    }

    public static void reset() {
        glowingEntityIds = Set.of();
        slimeBlockPositions = Set.of();
    }

    public static Set<BlockPos> getSlimeBlockPositions() {
        return slimeBlockPositions;
    }

    public static boolean shouldForceGlow(Entity entity) {
        return secretFinderEnabled && entity != null && glowingEntityIds.contains(entity.getId());
    }
}
