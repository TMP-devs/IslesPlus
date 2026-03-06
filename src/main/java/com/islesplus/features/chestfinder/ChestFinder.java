package com.islesplus.features.chestfinder;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ChestFinder {
    public static boolean chestFinderEnabled = false;
    public static float glowHue = 0.128f; // gold
    private static final Map<Integer, Boolean> modelCache = new HashMap<>();
    private static volatile Set<Integer> glowingEntityIds = Set.of();

    private ChestFinder() {
    }

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!chestFinderEnabled || WorldIdentification.world != PlayerWorld.RIFT || client.player == null || client.world == null) {
            glowingEntityIds = Set.of();
            modelCache.clear();
            return;
        }

        Set<Integer> nextGlowEntityIds = new HashSet<>();
        Set<Integer> seenIds = new HashSet<>();

        for (Entity display : scan.itemDisplays) {
            int displayId = display.getId();
            seenIds.add(displayId);

            Boolean cached = modelCache.get(displayId);
            boolean isChest;
            if (Boolean.TRUE.equals(cached)) {
                isChest = true;
            } else {
                isChest = isChestModel(display);
                if (isChest) modelCache.put(displayId, Boolean.TRUE);
            }
            if (!isChest) continue;

            nextGlowEntityIds.add(displayId);
        }

        modelCache.keySet().removeIf(key -> !seenIds.contains(key));
        glowingEntityIds = Set.copyOf(nextGlowEntityIds);
    }

    private static boolean isChestModel(Entity entity) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay)) return false;
        try {
            ItemStack stack = itemDisplay.getStackReference(0).get();
            if (stack.isEmpty()) return false;
            Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
            return model != null && model.getPath().contains("prop_crate");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void reset() {
        modelCache.clear();
        glowingEntityIds = Set.of();
    }

    public static boolean shouldForceGlow(Entity entity) {
        return chestFinderEnabled && entity != null && glowingEntityIds.contains(entity.getId());
    }
}
