package com.islesplus.screen;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import com.islesplus.mixin.ClickableWidgetAccessor;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Title-screen button from the "Join Isles Button" mockup (drawn at 2x there, so halved here).
 * JOIN is the oxblood button with a gold diamond either side of cream text; STONE is the grey
 * bevel the mockup gives the vanilla buttons, with Silkscreen text. A widget of its own because
 * vanilla buttons cannot be re-skinned (their drawing is final); it behaves like one - click,
 * Enter or Space, tab focus, narration.
 */
public class IslesMenuButton extends ClickableWidget {
    public enum Look { JOIN, STONE }

    private static final int INK = 0xFF15100A, TEXT_SHADOW = 0xFF2A1208;
    private static final int DIAMOND = 11, DIAMOND_GAP = 6;

    private final Look look;
    private final Consumer<AbstractInput> onPress;
    private boolean pressed;
    private ButtonWidget mirrored;

    public IslesMenuButton(int x, int y, int width, int height, Text message, Look look, Consumer<AbstractInput> onPress) {
        super(x, y, width, height, message);
        this.look = look;
        this.onPress = onPress;
    }

    /** Stands in for a vanilla button: carries its tooltip (the "why is this disabled" hints on
     * Multiplayer and Realms) and follows its active / visible state for as long as it lives. */
    public IslesMenuButton mirror(ButtonWidget original) {
        this.mirrored = original;
        this.active = original.active;
        this.visible = original.visible;
        Tooltip tip = ((ClickableWidgetAccessor) original).islesplus$getTooltipState().getTooltip();
        if (tip != null) setTooltip(tip);
        return this;
    }

    /** Called by the title screen before each frame (a hidden widget's own drawing never runs). */
    public void syncMirror() {
        if (mirrored != null) { this.active = mirrored.active; this.visible = mirrored.visible; }
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // The release can be swallowed (the click opened another screen, or the mouse went up
        // outside the window): never stay drawn pressed once the button itself is up.
        if (pressed && !Draw.leftMouseDown()) pressed = false;
        boolean lit = active && (isHovered() || isFocused());
        boolean down = pressed && isHovered();
        int x = getX() + 1, w = getWidth() - 2;                       // ring stays inside the slot
        int y = getY() + 1 + (down ? 2 : 0), h = getHeight() - 2 - (down ? 2 : 0);

        int fill, top, bottom, left, right, ring, text, diamond;
        if (look == Look.JOIN) {
            if (down)     { fill = 0xFF5D312A; top = 0xFF4A2A24; bottom = 0xFF6E3B33; left = fill; right = fill; ring = INK;        text = 0xFFE8DDC6; diamond = 0xFFC99A2A; }
            else if (lit) { fill = 0xFF7B453C; top = 0xFF96594E; bottom = 0xFF4A2A24; left = 0xFF8A4F45; right = 0xFF56302A; ring = 0xFFF2BC3C; text = 0xFFFFF6E0; diamond = 0xFFF8D268; }
            else          { fill = 0xFF6E3B33; top = 0xFF855045; bottom = 0xFF3F221D; left = 0xFF7B453C; right = 0xFF4A2A24; ring = INK;        text = 0xFFF6ECD4; diamond = 0xFFF2BC3C; }
        } else {
            fill = lit ? 0xFF7A7A7A : 0xFF6C6C6C; top = lit ? 0xFF9A9A9A : 0xFF8A8A8A; bottom = 0xFF4E4E4E;
            left = fill; right = fill; ring = lit ? 0xFFF6ECD4 : INK; text = lit ? 0xFFFFFFFF : 0xFFDCDCDC; diamond = 0;
            if (down) { int t = top; top = bottom; bottom = t; }
        }

        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, ring);
        ctx.fill(x, y, x + w, y + h, fill);
        ctx.fill(x, y, x + 1, y + h, left);
        ctx.fill(x + w - 1, y, x + w, y + h, right);
        ctx.fill(x, y, x + w, y + 1, top);
        ctx.fill(x, y + h - 1, x + w, y + h, bottom);

        String label = getMessage().getString();
        // Silkscreen only has Latin letters. A label in any other script (the vanilla buttons are
        // translated) is drawn in the game's own font, which has them, rather than as empty boxes.
        boolean latin = label.chars().allMatch(c -> c <= 0xFF);
        var vanilla = MinecraftClient.getInstance().textRenderer;
        int textW = latin ? Fonts.width(label) : vanilla.getWidth(label), textH = Fonts.GLYPH_H;
        int contentW = textW + (look == Look.JOIN ? 2 * (DIAMOND + DIAMOND_GAP) : 0);
        int cx = x + (w - contentW) / 2, cy = y + h / 2;
        if (look == Look.JOIN) {
            drawDiamond(ctx, cx + DIAMOND / 2, cy, diamond);
            cx += DIAMOND + DIAMOND_GAP;
        }
        int shadow = look == Look.JOIN ? TEXT_SHADOW : 0xFF2A2A2A;
        if (latin) {
            Fonts.draw(ctx, label, cx + 1, cy - textH / 2 + 1, shadow);
            Fonts.draw(ctx, label, cx, cy - textH / 2, text);
        } else {
            ctx.drawText(vanilla, label, cx + 1, cy - textH / 2 + 1, shadow, false);
            ctx.drawText(vanilla, label, cx, cy - textH / 2, text, false);
        }
        if (look == Look.JOIN) drawDiamond(ctx, cx + textW + DIAMOND_GAP + DIAMOND / 2, cy, diamond);
    }

    /** 11 px pixel diamond: ink outline, the state's gold, a lighter upper-left facet. */
    private static void drawDiamond(DrawContext ctx, int cx, int cy, int gold) {
        int r = DIAMOND / 2;
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            ctx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, TEXT_SHADOW);
            if (half >= 1) ctx.fill(cx - half + 1, cy + dy, cx + half, cy + dy + 1, gold);
        }
        ctx.fill(cx, cy - r, cx + 1, cy - r + 1, TEXT_SHADOW);
        ctx.fill(cx, cy + r, cx + 1, cy + r + 1, TEXT_SHADOW);
        int light = lighten(gold);
        for (int dy = -r + 2; dy <= -1; dy++) {
            int half = r - Math.abs(dy);
            ctx.fill(cx - half + 1, cy + dy, cx, cy + dy + 1, light);
        }
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 24), g = Math.min(255, ((argb >> 8) & 0xFF) + 24), b = Math.min(255, (argb & 0xFF) + 40);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        pressed = true;
        onPress.accept(click);
    }

    @Override
    public void onRelease(Click click) {
        pressed = false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!active || !visible || !isFocused()) return false;
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            playDownSound(MinecraftClient.getInstance().getSoundManager());
            onPress.accept(input);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
