package com.islesplus.mixin;

import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.playerfinder.PlayerFinder;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.features.waystonefinder.WaystoneFinder;
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
        if (ChestFinder.shouldForceGlow(self) || VendingMachineFinder.shouldForceGlow(self) || WaystoneFinder.shouldForceGlow(self) || SecretFinder.shouldForceGlow(self) || MobFinder.shouldForceGlow(self) || PlayerFinder.shouldForceGlow(self) || GroundItemsNotifier.shouldForceGlow(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void islesplus$forceFinderGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
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
        if (PlayerFinder.shouldForceGlow(self)) {
            cir.setReturnValue(PlayerFinder.glowRgb());
            return;
        }
        if (GroundItemsNotifier.shouldForceGlow(self)) {
            cir.setReturnValue(MathHelper.hsvToRgb(GroundItemsNotifier.glowHue, 1.0f, 1.0f));
        }
    }
}
