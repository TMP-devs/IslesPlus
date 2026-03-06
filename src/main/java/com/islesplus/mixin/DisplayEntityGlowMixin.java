package com.islesplus.mixin;

import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DisplayEntity overrides Entity.getTeamColorValue() with its own GLOW_COLOR_OVERRIDE
 * tracked data, so the EntityGlowMixin on Entity.class never fires for display entities.
 * This mixin targets DisplayEntity directly to apply our finder colors.
 */
@Mixin(DisplayEntity.class)
public class DisplayEntityGlowMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void islesplus$forceDisplayGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (SecretFinder.shouldForceGlow(self)) {
            cir.setReturnValue(MathHelper.hsvToRgb(SecretFinder.glowHue, 1.0f, 1.0f));
            return;
        }
        if (ChestFinder.shouldForceGlow(self)) {
            cir.setReturnValue(MathHelper.hsvToRgb(ChestFinder.glowHue, 1.0f, 1.0f));
            return;
        }
        if (VendingMachineFinder.shouldForceGlow(self)) {
            cir.setReturnValue(MathHelper.hsvToRgb(VendingMachineFinder.glowHue, 1.0f, 1.0f));
            return;
        }
        if (MobFinder.shouldForceGlow(self)) {
            cir.setReturnValue(MathHelper.hsvToRgb(MobFinder.glowHue, 1.0f, 1.0f));
        }
    }
}
