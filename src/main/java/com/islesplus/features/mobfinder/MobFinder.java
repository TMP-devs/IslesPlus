package com.islesplus.features.mobfinder;

import com.islesplus.IslesPlusConfig;
import com.islesplus.entity.EntityScanResult;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.features.voidcrystalfinder.VoidCrystalFinder;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MobFinder {
    private static final double PAIR_DISTANCE = 2.0;
    private static final double TEXT_REQUIRED_DISTANCE = 3.0;
    private static final double EXCLUSION_XZ = 0.5;
    private static final double EXCLUSION_Y = 2.0;
    private static final long STABLE_MATCH_MS = 300L;

    public static boolean mobFinderEnabled = false;
    public static float glowHue = 0.617f; // blue
    private static final java.util.Map<String, Long> pairFirstSeenMs = new java.util.HashMap<>();
    private static volatile Set<Integer> glowingEntityIds = Set.of();

    private MobFinder() {
    }

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!mobFinderEnabled || WorldIdentification.world != PlayerWorld.RIFT || client.player == null || client.world == null) {
            glowingEntityIds = Set.of();
            pairFirstSeenMs.clear();
            return;
        }

        // Collect exclusion zones around "Exit Rift" text displays
        List<double[]> exclusionPositions = new ArrayList<>();
        for (Entity entity : scan.textDisplaysFar) {
            if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay) {
                String text = textDisplay.getText().getString().toLowerCase(Locale.ROOT).trim();
                if (text.equals("exit rift")) {
                    exclusionPositions.add(new double[]{entity.getX(), entity.getY(), entity.getZ()});
                }
            }
        }

        double pairDistanceSq = PAIR_DISTANCE * PAIR_DISTANCE;
        double textRequiredDistanceSq = TEXT_REQUIRED_DISTANCE * TEXT_REQUIRED_DISTANCE;
        long nowMs = System.currentTimeMillis();
        Set<String> activePairs = new HashSet<>();
        Set<Integer> nextGlowEntityIds = new HashSet<>();
        for (Entity display : scan.itemDisplays) {
            for (Entity cloud : scan.areaEffectClouds) {
                if (display.squaredDistanceTo(cloud) > pairDistanceSq) {
                    continue;
                }
                if (!hasNearbyTextDisplay(scan.textDisplaysFar, display, cloud, textRequiredDistanceSq)) {
                    continue;
                }
                if (isNearExclusionZone(display, exclusionPositions) || isNearExclusionZone(cloud, exclusionPositions)) {
                    continue;
                }
                if (VendingMachineFinder.isVendingMachineEntity(display) || VendingMachineFinder.isVendingMachineEntity(cloud)) {
                    continue;
                }
                if (VoidCrystalFinder.isVoidCrystalEntity(display) || VoidCrystalFinder.isVoidCrystalEntity(cloud)) {
                    continue;
                }
                String pairKey = pairKey(display.getId(), cloud.getId());
                activePairs.add(pairKey);
                long firstSeen = pairFirstSeenMs.getOrDefault(pairKey, nowMs);
                pairFirstSeenMs.putIfAbsent(pairKey, nowMs);
                if (nowMs - firstSeen >= STABLE_MATCH_MS) {
                    nextGlowEntityIds.add(display.getId());
                    nextGlowEntityIds.add(cloud.getId());
                }
                if (activePairs.size() >= IslesPlusConfig.maxMatches) {
                    break;
                }
            }
            if (activePairs.size() >= IslesPlusConfig.maxMatches) {
                break;
            }
        }

        pairFirstSeenMs.keySet().removeIf(key -> !activePairs.contains(key));
        glowingEntityIds = Set.copyOf(nextGlowEntityIds);
    }

    private static boolean hasNearbyTextDisplay(List<Entity> nearby, Entity display, Entity cloud, double textDistanceSq) {
        for (Entity entity : nearby) {
            if (entity.squaredDistanceTo(display) <= textDistanceSq || entity.squaredDistanceTo(cloud) <= textDistanceSq) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNearExclusionZone(Entity entity, List<double[]> exclusionPositions) {
        for (double[] pos : exclusionPositions) {
            double dx = Math.abs(entity.getX() - pos[0]);
            double dy = Math.abs(entity.getY() - pos[1]);
            double dz = Math.abs(entity.getZ() - pos[2]);
            if (dx <= EXCLUSION_XZ && dz <= EXCLUSION_XZ && dy <= EXCLUSION_Y) {
                return true;
            }
        }
        return false;
    }

    private static String pairKey(int firstId, int secondId) {
        if (firstId <= secondId) {
            return firstId + ":" + secondId;
        }
        return secondId + ":" + firstId;
    }

    public static void reset() {
        pairFirstSeenMs.clear();
        glowingEntityIds = Set.of();
    }

    public static boolean shouldForceGlow(Entity entity) {
        return mobFinderEnabled && entity != null && glowingEntityIds.contains(entity.getId());
    }
}
