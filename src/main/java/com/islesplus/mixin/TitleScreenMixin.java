package com.islesplus.mixin;

import com.islesplus.IslesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import com.islesplus.screen.IslesMenuButton;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final String ISLES_HOST = "play.skyblockisles.net";

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    /** Vanilla stacks its main buttons 24 px apart (20 px button + 4 px gap). */
    private static final int ROW_STEP = 24;
    /** Same height as the vanilla buttons, so the column stays even. */
    private static final int JOIN_H = 20;

    @Inject(method = "init", at = @At("TAIL"))
    private void islesplus$restyleTitleMenu(CallbackInfo ci) {
        ClickableWidget singleplayer = null;
        for (Element child : this.children()) {
            if (child instanceof ClickableWidget w && islesplus$isSingleplayer(w.getMessage())) {
                singleplayer = w;
                break;
            }
        }
        boolean plainMenu = singleplayer != null;
        for (Element child : this.children()) plainMenu &= child instanceof ClickableWidget;
        // The rebuild below re-adds every child as drawn + clickable. A draw-only or click-only
        // element (another mod's) would be lost or changed by that, so such a menu is left alone too.
        List<Drawable> drawn = ((ScreenAccessor) this).islesplus$getDrawables();
        plainMenu &= drawn.size() == this.children().size() && this.children().containsAll(drawn);
        if (!plainMenu) {
            // Demo mode, or another mod rebuilt the menu: leave it alone, Join goes top-left.
            this.addDrawableChild(islesplus$join(8, 8, 170, JOIN_H));
            return;
        }

        // Singleplayer moves UP one row and Join takes its old slot. Nothing below moves, so
        // Multiplayer, Realms (whose notification icons vanilla draws at a fixed height), Options,
        // Quit and the footer text all stay exactly where the game and other mods expect them.
        int slotX = singleplayer.getX(), slotY = singleplayer.getY(), slotW = singleplayer.getWidth();
        singleplayer.setY(slotY - ROW_STEP);
        int columnBottom = slotY + ROW_STEP * 3 + 12;   // the Options / Quit row

        // Rebuild the child list in its original order, so Tab still walks the menu top to bottom:
        // plain text buttons in the main column become stone-look twins, Join follows Singleplayer.
        List<ClickableWidget> ordered = new ArrayList<>();
        for (Element child : this.children()) {
            ClickableWidget w = (ClickableWidget) child;   // checked above
            boolean inColumn = w.getX() >= slotX - 1 && w.getX() + w.getWidth() <= slotX + slotW + 1
                && w.getY() >= slotY - ROW_STEP && w.getY() <= columnBottom;
            if (inColumn && w instanceof ButtonWidget original && !(w instanceof TextIconButtonWidget)) {
                ordered.add(new IslesMenuButton(w.getX(), w.getY(), w.getWidth(), w.getHeight(),
                    w.getMessage(), IslesMenuButton.Look.STONE, original::onPress).mirror(original));
            } else {
                ordered.add(w);
            }
            if (w == singleplayer) ordered.add(islesplus$join(slotX, slotY, slotW, JOIN_H));
        }
        this.clearChildren();
        for (ClickableWidget w : ordered) this.addDrawableChild(w);
    }

    /** A hidden widget is never drawn, so a twin cannot notice its original coming back from
     * inside its own drawing: the screen keeps the twins in step before each frame. */
    @Inject(method = "render", at = @At("HEAD"))
    private void islesplus$syncTwins(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        for (Element child : this.children()) {
            if (child instanceof IslesMenuButton twin) twin.syncMirror();
        }
    }

    /** By translation key, not by the rendered words: a resource pack or language cannot hide it. */
    private static boolean islesplus$isSingleplayer(Text message) {
        return message.getContent() instanceof TranslatableTextContent t && "menu.singleplayer".equals(t.getKey());
    }

    private IslesMenuButton islesplus$join(int x, int y, int width, int height) {
        IslesMenuButton join = new IslesMenuButton(x, y, width, height, Text.literal("Join Isles"),
            IslesMenuButton.Look.JOIN, input -> islesplus$connectToIsles());
        join.setTooltip(Tooltip.of(Text.literal("Quick connect: " + ISLES_HOST)));
        return join;
    }

    private void islesplus$connectToIsles() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerAddress address = ServerAddress.parse(ISLES_HOST);
        ServerInfo info = new ServerInfo("Skyblock Isles", ISLES_HOST, ServerInfo.ServerType.OTHER);
        IslesClient.connectingToIsles = true;
        ConnectScreen.connect(this, client, address, info, false, null);
    }
}
