package com.islesplus.features.vendingmachinefinder;

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

public final class VendingMachineFinder {
    public static boolean vendingMachineFinderEnabled = false;
    public static float glowHue = 0.092f; // orange
    private static final Map<Integer, Boolean> modelCache = new HashMap<>();
    private static final Map<Integer, Boolean> activeCache = new HashMap<>();
    private static volatile Set<Integer> identifiedMachineIds = Set.of();
    private static volatile Set<Integer> glowingEntityIds = Set.of();

    private VendingMachineFinder() {}

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        // Always detect when in rift so other finders can filter out vending machines
        if (WorldIdentification.world != PlayerWorld.RIFT
                || client.player == null || client.world == null) {
            identifiedMachineIds = Set.of();
            glowingEntityIds = Set.of();
            modelCache.clear();
            activeCache.clear();
            return;
        }

        Set<Integer> allMachines = new HashSet<>();
        Set<Integer> nextGlowEntityIds = new HashSet<>();
        Set<Integer> seenIds = new HashSet<>();

        for (Entity display : scan.itemDisplays) {
            int displayId = display.getId();
            seenIds.add(displayId);

            Boolean cachedModel = modelCache.get(displayId);
            boolean isMachine;
            if (Boolean.TRUE.equals(cachedModel)) {
                isMachine = true;
            } else {
                isMachine = isAnyVendingMachineModel(display);
                if (isMachine) modelCache.put(displayId, Boolean.TRUE);
            }
            if (!isMachine) continue;

            allMachines.add(displayId);

            Boolean cachedActive = activeCache.get(displayId);
            boolean isActive;
            if (Boolean.TRUE.equals(cachedActive)) {
                isActive = true;
            } else {
                isActive = isActiveVendingMachineModel(display);
                if (isActive) activeCache.put(displayId, Boolean.TRUE);
            }
            if (!isActive) continue;

            nextGlowEntityIds.add(displayId);
        }

        modelCache.keySet().removeIf(key -> !seenIds.contains(key));
        activeCache.keySet().removeIf(key -> !seenIds.contains(key));
        identifiedMachineIds = Set.copyOf(allMachines);
        glowingEntityIds = Set.copyOf(nextGlowEntityIds);
    }

    /** Matches any vending machine model (active or inactive). Used for identification/filtering. */
    private static boolean isAnyVendingMachineModel(Entity entity) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay)) return false;
        try {
            ItemStack stack = itemDisplay.getStackReference(0).get();
            if (stack.isEmpty()) return false;
            Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
            return model != null && model.getPath().contains("prop_snack_machine");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Matches only active vending machine models. Used for glow eligibility. */
    private static boolean isActiveVendingMachineModel(Entity entity) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay)) return false;
        try {
            ItemStack stack = itemDisplay.getStackReference(0).get();
            if (stack.isEmpty()) return false;
            Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
            return model != null && model.getPath().contains("prop_snack_machine")
                    && !model.getPath().contains("prop_snack_machine_inactive");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void reset() {
        modelCache.clear();
        activeCache.clear();
        identifiedMachineIds = Set.of();
        glowingEntityIds = Set.of();
    }

    /** Returns true if the entity's model matches a vending machine. */
    public static boolean isVendingMachineEntity(Entity entity) {
        return entity != null && identifiedMachineIds.contains(entity.getId());
    }

    public static boolean shouldForceGlow(Entity entity) {
        return vendingMachineFinderEnabled && entity != null && glowingEntityIds.contains(entity.getId());
    }
}
