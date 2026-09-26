package com.islesplus.features.vendingmachinefinder;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.ui.GlowColor;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import com.islesplus.sync.FeatureFlags;
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
    public static float glowSaturation = 1.0f;
    public static float glowLightness = 0.5f;
    private static final Map<Integer, Boolean> modelCache = new HashMap<>();
    private static final Map<Integer, Boolean> activeCache = new HashMap<>();
    private static volatile Set<Integer> identifiedMachineIds = Set.of();
    private static volatile Set<Integer> glowingEntityIds = Set.of();

    private VendingMachineFinder() {}

    public static int glowRgb() {
        return GlowColor.rgb(glowHue, glowSaturation, glowLightness);
    }

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        // always scan in rifts even if the feature's off, other finders need this to skip vending machines
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

    /** any vending machine, active or not. used to identify them for filtering */
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

    /** only the active ones, those are the ones that get the glow */
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

    /** is this thing a vending machine */
    public static boolean isVendingMachineEntity(Entity entity) {
        return entity != null && identifiedMachineIds.contains(entity.getId());
    }

    public static boolean shouldForceGlow(Entity entity) {
        // the remote check matters: a killed finder's tick stops, so its set keeps the last entities
        return vendingMachineFinderEnabled && !FeatureFlags.isKilled("vending_machine_finder") && entity != null && glowingEntityIds.contains(entity.getId());
    }
}
