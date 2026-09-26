package com.islesplus.mixin;

import com.islesplus.features.berryalert.BerryAlert;
import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.harvestables.HarvestableHighlighter;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.features.waystonefinder.WaystoneFinder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DisplayEntity overrides getTeamColorValue() with its own GLOW_COLOR_OVERRIDE thing,
 * so our EntityGlowMixin on Entity never fires for display entities. this one hits
 * DisplayEntity directly so the finder colors actually show up
 */
@Mixin(DisplayEntity.class)
public class DisplayEntityGlowMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void islesplus$forceDisplayGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (BerryAlert.shouldForceGlow(self)) {
            cir.setReturnValue(BerryAlert.glowRgb());
            return;
        }
        if (SecretFinder.shouldForceGlow(self)) {
            cir.setReturnValue(SecretFinder.glowRgb());
            return;
        }
        if (ChestFinder.shouldForceGlow(self)) {
            cir.setReturnValue(ChestFinder.glowRgb());
            return;
        }
        if (VendingMachineFinder.shouldForceGlow(self)) {
            cir.setReturnValue(VendingMachineFinder.glowRgb());
            return;
        }
        if (WaystoneFinder.shouldForceGlow(self)) {
            cir.setReturnValue(WaystoneFinder.glowRgb());
            return;
        }
        if (MobFinder.shouldForceGlow(self)) {
            cir.setReturnValue(MobFinder.glowRgb());
            return;
        }
        if (HarvestableHighlighter.shouldForceGlow(self)) {
            cir.setReturnValue(HarvestableHighlighter.glowRgb(self));
        }
    }
}
