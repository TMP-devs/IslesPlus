package com.islesplus.features.slotlocker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.islesplus.IslesPlusConfig;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;

import java.util.HashSet;
import java.util.Set;

public class SlotLocker {
    public static boolean slotLockEnabled = false;
    private static final Set<Integer> lockedSlots = new HashSet<>();

    /** Called by IslesPlusConfig.load() to restore persisted locked slots. */
    public static void setLockedSlots(JsonArray arr) {
        lockedSlots.clear();
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                lockedSlots.add(e.getAsInt());
            }
        }
    }

    /** Called by IslesPlusConfig.save() to serialize the current locked slots. */
    public static JsonArray getLockedSlotsJson() {
        JsonArray arr = new JsonArray();
        lockedSlots.forEach(arr::add);
        return arr;
    }

    /**
     * Returns true if this slot belongs to the player's inventory and is locked.
     * Uses inventory reference identity so it works regardless of which screen is open.
     */
    public static boolean isLocked(Slot slot, PlayerInventory playerInventory) {
        if (!slotLockEnabled || WorldIdentification.world == PlayerWorld.OTHER) return false;
        return slot.inventory == playerInventory && lockedSlots.contains(slot.getIndex());
    }

    /**
     * Returns true if the given hotbar index (0–8) is locked.
     * Hotbar indices 0–8 in PlayerInventory match the output of GameOptions.getHotbarIndex().
     */
    public static boolean isHotbarSlotLocked(int hotbarIndex) {
        if (!slotLockEnabled || WorldIdentification.world == PlayerWorld.OTHER) return false;
        return lockedSlots.contains(hotbarIndex);
    }

    /** Toggles the lock on the given slot. No-op if the slot is not a player inventory slot. */
    public static boolean toggleLock(Slot slot, PlayerInventory playerInventory) {
        if (!slotLockEnabled || WorldIdentification.world == PlayerWorld.OTHER) return false;
        if (slot.inventory != playerInventory) return false;
        int index = slot.getIndex();
        if (!lockedSlots.remove(index)) {
            lockedSlots.add(index);
        }
        IslesPlusConfig.save();
        return true;
    }

    /** Called on disconnect. Locks are intentionally preserved across sessions. */
    public static void reset() {}
}
