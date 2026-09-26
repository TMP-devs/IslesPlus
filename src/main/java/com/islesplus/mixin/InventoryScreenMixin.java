package com.islesplus.mixin;

import com.islesplus.features.quickactions.QuickActions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the quick action buttons at the end of the inventory's own render pass: in front of the
 * panel and the item icons, but before the hovered item's tooltip, which the screen paints after
 * render returns.
 *
 * <p>This has to target {@link InventoryScreen} itself. {@code RecipeBookScreen} - which it
 * extends - overrides {@code render} without calling {@code super}, so an inject on
 * {@code HandledScreen.render} never fires for the inventory.
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    private void islesplus$drawQuickActions(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        QuickActions.render((InventoryScreen) (Object) this, context, mouseX, mouseY);
    }
}
