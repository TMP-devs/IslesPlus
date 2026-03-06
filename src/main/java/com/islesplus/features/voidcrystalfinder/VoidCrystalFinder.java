package com.islesplus.features.voidcrystalfinder;

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

public final class VoidCrystalFinder {

    private static final Map<Integer, Boolean> modelCache = new HashMap<>();
    private static volatile Set<Integer> identifiedCrystalIds = Set.of();

    private VoidCrystalFinder() {}

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (WorldIdentification.world != PlayerWorld.RIFT
                || client.player == null || client.world == null) {
            identifiedCrystalIds = Set.of();
            modelCache.clear();
            return;
        }

        Set<Integer> activeCrystals = new HashSet<>();
        Set<Integer> seenIds = new HashSet<>();

        for (Entity display : scan.itemDisplays) {
            int displayId = display.getId();
            seenIds.add(displayId);

            Boolean cached = modelCache.get(displayId);
            boolean isCrystal;
            if (Boolean.TRUE.equals(cached)) {
                isCrystal = true;
            } else {
                isCrystal = isVoidCrystalModel(display);
                if (isCrystal) modelCache.put(displayId, Boolean.TRUE);
            }
            if (isCrystal) activeCrystals.add(displayId);
        }

        modelCache.keySet().removeIf(key -> !seenIds.contains(key));
        identifiedCrystalIds = Set.copyOf(activeCrystals);
    }

    private static boolean isVoidCrystalModel(Entity entity) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay)) return false;
        try {
            ItemStack stack = itemDisplay.getStackReference(0).get();
            if (stack.isEmpty()) return false;
            Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
            return model != null && model.getPath().contains("prop_void_crystal_framed");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void reset() {
        modelCache.clear();
        identifiedCrystalIds = Set.of();
    }

    /** Returns true if the entity's model matches a void crystal. */
    public static boolean isVoidCrystalEntity(Entity entity) {
        return entity != null && identifiedCrystalIds.contains(entity.getId());
    }
}
