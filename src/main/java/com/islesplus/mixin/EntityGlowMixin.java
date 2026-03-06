package com.islesplus.mixin;

import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.playerfinder.PlayerFinder;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void islesplus$forceFinderGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (ChestFinder.shouldForceGlow(self) || VendingMachineFinder.shouldForceGlow(self) || SecretFinder.shouldForceGlow(self) || MobFinder.shouldForceGlow(self) || PlayerFinder.shouldForceGlow(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void islesplus$forceFinderGlowColor(CallbackInfoReturnable<Integer> cir) {
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
            return;
        }
        if (PlayerFinder.shouldForceGlow(self)) {
            cir.setReturnValue(MathHelper.hsvToRgb(PlayerFinder.glowHue, 1.0f, 1.0f));
        }
    }
}
