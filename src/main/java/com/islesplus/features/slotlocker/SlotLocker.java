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
    public static boolean slotLockEnabled = true;
    private static final Set<Integer> lockedSlots = new HashSet<>();

    /** config load calls this to restore locked slots */
    public static void setLockedSlots(JsonArray arr) {
        lockedSlots.clear();
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                lockedSlots.add(e.getAsInt());
            }
        }
    }

    /** config save calls this to write them out */
    public static JsonArray getLockedSlotsJson() {
        JsonArray arr = new JsonArray();
        lockedSlots.forEach(arr::add);
        return arr;
    }

    /**
     * is this one of the player's own slots and locked?
     * compares the inventory reference so it works in chests, crafting tables, whatever
     */
    public static boolean isLocked(Slot slot, PlayerInventory playerInventory) {
        if (!slotLockEnabled || WorldIdentification.world == PlayerWorld.OTHER) return false;
        return slot.inventory == playerInventory && lockedSlots.contains(slot.getIndex());
    }

    /**
     * is this hotbar slot (0-8) locked?
     * PlayerInventory hotbar indices line up with GameOptions.getHotbarIndex()
     */
    public static boolean isHotbarSlotLocked(int hotbarIndex) {
        if (!slotLockEnabled || WorldIdentification.world == PlayerWorld.OTHER) return false;
        return lockedSlots.contains(hotbarIndex);
    }

    /** flip the lock on a slot. does nothing if it's not a player inventory slot */
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

    /** on disconnect. locks stay locked on purpose, they're saved to config */
    public static void reset() {}
}
