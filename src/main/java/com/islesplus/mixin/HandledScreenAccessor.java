package com.islesplus.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("focusedSlot")
    @Nullable Slot getFocusedSlot();

    @Accessor("handler")
    ScreenHandler getHandler();

    @Accessor("x")
    int getGuiX();

    @Accessor("y")
    int getGuiY();

    @Accessor("backgroundWidth")
    int getGuiWidth();
}
