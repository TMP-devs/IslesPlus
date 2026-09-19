package com.islesplus.mixin;

import net.minecraft.client.gui.tooltip.TooltipState;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads a widget's tooltip, so a re-skinned twin of a vanilla button can carry the same one. */
@Mixin(ClickableWidget.class)
public interface ClickableWidgetAccessor {
    @Accessor("tooltip")
    TooltipState islesplus$getTooltipState();
}
