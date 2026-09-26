package com.islesplus.mixin;

import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.slotlocker.SlotLocker;
import com.islesplus.features.storagecount.StorageSlotLabel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

        // locked slot, don't let them touch it
        if (slot != null && SlotLocker.isLocked(slot, client.player.getInventory())) {
            ci.cancel();
            return;
        }

        // number key swap into a locked hotbar slot, nope
        if (actionType == SlotActionType.SWAP && button >= 0 && button < 9 && SlotLocker.isHotbarSlotLocked(button)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V",
        at = @At("TAIL")
    )
    private void islesplus$drawSlotOverlays(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (islesplus$storageLabel != null) {
            StorageSlotLabel.draw(context, islesplus$storageLabel, slot.x, slot.y);
            islesplus$storageLabel = null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && SlotLocker.isLocked(slot, client.player.getInventory())) {
            drawLockIcon(context, slot.x, slot.y);
        }
        InventorySearch.drawSlotOverlay(context, slot);
    }

    /** The storage amount of the slot being drawn, set where vanilla would draw its count. */
    private String islesplus$storageLabel;

    /** A storage slot shows what it really holds (its "Stored:" line), not the stack's 99 - drawn
     * smaller by us at the end of the slot (vanilla's count is blanked), so a three-digit amount
     * fits its own slot instead of running into the next one's. */
    @ModifyArg(
        method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V"),
        index = 4
    )
    private String islesplus$storedCount(TextRenderer textRenderer, ItemStack stack, int x, int y, String countLabel) {
        islesplus$storageLabel = countLabel == null ? StorageSlotLabel.of(stack, null) : null;
        return islesplus$storageLabel != null ? "" : countLabel;
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
