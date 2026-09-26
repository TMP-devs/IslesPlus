package com.islesplus.mixin;

import com.islesplus.features.berryalert.BerryAlert;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier;
import com.islesplus.features.harvestables.HarvestableHighlighter;
import com.islesplus.features.playerfinder.PlayerFinder;
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
        if (BerryAlert.shouldForceGlow(self) || WaystoneFinder.shouldForceGlow(self) || PlayerFinder.shouldForceGlow(self) || GroundItemsNotifier.shouldForceGlow(self) || HarvestableHighlighter.shouldForceGlow(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void islesplus$forceFinderGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (WaystoneFinder.shouldForceGlow(self)) {
            cir.setReturnValue(WaystoneFinder.glowRgb());
            return;
        }
        if (PlayerFinder.shouldForceGlow(self)) {
            cir.setReturnValue(PlayerFinder.glowRgb());
            return;
        }
        if (HarvestableHighlighter.shouldForceGlow(self)) {
            cir.setReturnValue(HarvestableHighlighter.glowRgb(self));
            return;
        }
        if (GroundItemsNotifier.shouldForceGlow(self)) {
            cir.setReturnValue(MathHelper.hsvToRgb(GroundItemsNotifier.glowHue, 1.0f, 1.0f));
        }
    }
}
