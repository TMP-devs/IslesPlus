package com.islesplus.screen;

import com.islesplus.ui.Fonts;
import com.islesplus.IslesPlusConfig;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A bare screen that only hosts kit overlays (dialogs, dropdown lists), for opening an Isles+
 * pop-up from somewhere that is not the /ip screen, e.g. the inventory. When the last overlay
 * closes, the screen closes too and runs {@code onClose} (typically: reopen what was there).
 */
public class OverlayScreen extends Screen implements OverlayHost {
    private final List<Widget> overlays = new ArrayList<>();
    private final Runnable onClose;
    private boolean closing;

    public OverlayScreen(Runnable onClose) {
        super(Text.literal("Isles+"));
        this.onClose = onClose;
    }

    @Override public boolean shouldPause() { return false; }

    private void renderUi(DrawContext ctx, int mouseX, int mouseY, float delta) {
        for (Widget overlay : new ArrayList<>(overlays)) {
            ctx.createNewRootLayer();   // item icons in a lower overlay must not show through
            overlay.layout(0, 0, this.width);
            overlay.render(ctx, mouseX, mouseY);
        }
    }

    // ==============================
    // Overlay host
    // ==============================

    @Override public void openOverlay(Widget overlay) { overlays.add(overlay); }

    /** Same contract as IslesScreen: removes the overlay and everything above it, unfocusing each. */
    @Override public void closeOverlay(Widget overlay) {
        int i = overlays.indexOf(overlay);
        if (i < 0) return;
        while (overlays.size() > i) overlays.remove(overlays.size() - 1).unfocus();
        if (overlays.isEmpty()) close();
    }

    @Override public int screenWidth() { return this.width; }

    @Override public int screenHeight() { return this.height; }

    @Override public void closeScreen() { close(); }

    private Widget top() { return overlays.isEmpty() ? null : overlays.get(overlays.size() - 1); }

    // ==============================
    // Input
    // ==============================

    private boolean mouseClickedUi(Click click, boolean doubled) {
        Widget top = top();
        if (top != null) top.mouseClicked(click.x(), click.y(), click.button());
        return true;
    }

    private boolean mouseDraggedUi(Click click, double deltaX, double deltaY) {
        Widget top = top();
        if (top != null) top.mouseDragged(click.x(), click.y());
        return true;
    }

    private boolean mouseReleasedUi(Click click) {
        Widget top = top();
        if (top != null) top.mouseReleased();
        return true;
    }

    private boolean mouseScrolledUi(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Widget top = top();
        if (top != null) top.mouseScrolled(mouseX, mouseY, verticalAmount);
        return true;
    }

    private boolean keyPressedUi(KeyInput input) {
        Widget top = top();
        if (top == null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) close();
            return true;
        }
        if (!top.keyPressed(input) && input.key() == GLFW.GLFW_KEY_ESCAPE) closeOverlay(top);
        return true;
    }

    private boolean charTypedUi(CharInput input) {
        Widget top = top();
        if (top != null) top.charTyped(input);
        return true;
    }

    @Override public void removed() {
        for (Widget overlay : new ArrayList<>(overlays)) overlay.unfocus();
        IslesPlusConfig.save();
        super.removed();
    }

    @Override public void close() {
        if (closing) return;
        closing = true;
        for (Widget overlay : new ArrayList<>(overlays)) overlay.unfocus();
        overlays.clear();
        onClose.run();
    }

    // /ip UI: its text is in the Isles+ faces (Fonts.islesUi), everything else's in the game's font.
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) { Fonts.islesUi(() -> renderUi(ctx, mouseX, mouseY, delta)); }
    @Override public boolean mouseClicked(Click click, boolean doubled) { return Fonts.islesUi(() -> mouseClickedUi(click, doubled)); }
    @Override public boolean mouseDragged(Click click, double deltaX, double deltaY) { return Fonts.islesUi(() -> mouseDraggedUi(click, deltaX, deltaY)); }
    @Override public boolean mouseReleased(Click click) { return Fonts.islesUi(() -> mouseReleasedUi(click)); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) { return Fonts.islesUi(() -> mouseScrolledUi(mouseX, mouseY, horizontalAmount, verticalAmount)); }
    @Override public boolean keyPressed(KeyInput input) { return Fonts.islesUi(() -> keyPressedUi(input)); }
    @Override public boolean charTyped(CharInput input) { return Fonts.islesUi(() -> charTypedUi(input)); }
}
