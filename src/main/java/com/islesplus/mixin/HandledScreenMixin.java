package com.islesplus.mixin;

import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.slotlocker.SlotLocker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(
        method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void islesplus$blockLockedSlotClicks(
        Slot slot,
        int slotId, // required by target signature; position must match
        int button,
        SlotActionType actionType,
        CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Block any direct interaction with a locked player-inventory slot.
        if (slot != null && SlotLocker.isLocked(slot, client.player.getInventory())) {
            ci.cancel();
            return;
        }

        // Block hotbar swaps if the target hotbar slot is locked.
        if (actionType == SlotActionType.SWAP && button >= 0 && button < 9 && SlotLocker.isHotbarSlotLocked(button)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V",
        at = @At("TAIL")
    )
    private void islesplus$drawSlotOverlays(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && SlotLocker.isLocked(slot, client.player.getInventory())) {
            drawLockIcon(context, slot.x, slot.y);
        }
        InventorySearch.drawSlotOverlay(context, slot);
    }

    private void drawLockIcon(DrawContext context, int ox, int oy) {
        final int W = 0x88FFD700; // transparent gold (~53% opacity)
        final int D = 0x88000000; // transparent dark for keyhole

        // Shackle: top bar
        context.fill(ox + 4, oy + 2, ox + 12, oy + 3, W);
        // Shackle: left leg
        context.fill(ox + 4, oy + 2, ox + 6, oy + 7, W);
        // Shackle: right leg
        context.fill(ox + 10, oy + 2, ox + 12, oy + 7, W);

        // Body (filled rectangle)
        context.fill(ox + 3, oy + 6, ox + 13, oy + 14, W);

        // Keyhole cutout: dark area in center of body
        context.fill(ox + 7, oy + 8, ox + 9, oy + 13, D);
    }
}
