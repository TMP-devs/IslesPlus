package com.islesplus.mixin;

import com.islesplus.screen.IslesMenuButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The menu itself is restyled by {@link com.islesplus.screen.TitleMenuRestyle} (a Fabric screen
 * event, so it runs after other mods); this only keeps the stone twins in step each frame. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    /** A hidden widget is never drawn, so a twin cannot notice its original coming back from
     * inside its own drawing: the screen keeps the twins in step before each frame. */
    @Inject(method = "render", at = @At("HEAD"))
    private void islesplus$syncTwins(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        for (Element child : this.children()) {
            if (child instanceof IslesMenuButton twin) twin.syncMirror();
        }
    }
}
