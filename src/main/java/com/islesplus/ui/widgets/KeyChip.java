package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;


/** 14-high keybind chip, sized to its text (at least 48 wide): shows the bound key, "Unbound" or, while listening for the next
 * press, "Press a key". Clicking the chip toggles listening; while listening any key or mouse
 * button rebinds it (Escape unbinds), consuming the input either way.
 * <p>Contract: a host must deliver every left click either to this widget or call
 * {@code unfocus()} on it, so a click elsewhere always cancels listening; Flow containers
 * already do this for siblings. It must also deliver non-left clicks, this is the only widget
 * in the kit that wants them, and the only way a mouse button can be bound at all. Those clicks
 * must arrive WITHOUT the usual unfocus sweeps around them, or the chip would be cancelled by
 * the very click it is waiting for; {@link com.islesplus.ui.Flow} and the /ip screen both run
 * those sweeps for button 0 only. */
public class KeyChip extends Widget {
    private final KeyBinding binding;
    private boolean listening = false;

    public KeyChip(KeyBinding binding) {
        this.binding = binding;
    }

    private static final int MIN_W = 48, PAD_X = 4;
    private static final String LISTENING_LABEL = "Press a key", UNBOUND_LABEL = "Unbound";

    /** Wide enough for the longest thing this chip can say - its listening prompt or its current
     * key name - at the text size the GUI scale really gives (SMALL snaps to full size at GUI
     * scales 1 and 2). Never below 48, so the usual one-letter keys keep the familiar chip. */
    @Override public int prefWidth() {
        int widest = Math.max(Fonts.width(LISTENING_LABEL, Fonts.SMALL), Fonts.width(keyLabel(), Fonts.SMALL));
        return Math.max(MIN_W, widest + 2 * PAD_X);
    }

    private String keyLabel() {
        return binding.isUnbound() ? UNBOUND_LABEL : binding.getBoundKeyLocalizedText().getString();
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.w = Math.min(prefWidth(), Math.max(0, width));   // a narrow card ellipsizes the label instead
        this.h = 14;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        int fillC, litC, shadeC, ringC, textC;
        String label;
        if (listening) {
            fillC = Theme.OXBLOOD; litC = Theme.OXBLOOD_LIT; shadeC = Theme.OXBLOOD_SHADE; ringC = Theme.INK; textC = Theme.CREAM;
            label = LISTENING_LABEL;
        } else if (binding.isUnbound()) {
            fillC = Theme.DISABLED; litC = Theme.DISABLED_LIT; shadeC = Theme.DISABLED_SHADE; ringC = Theme.DISABLED_RING; textC = Theme.DISABLED_TEXT;
            label = UNBOUND_LABEL;
        } else {
            fillC = Theme.RAISED; litC = Theme.RAISED_LIT; shadeC = Theme.RAISED_SHADE; ringC = Theme.RAISED_RING; textC = Theme.TEXT_LABEL;
            label = keyLabel();
        }

        Draw.bevel(ctx, x, y, w, h, fillC, litC, shadeC, ringC);

        float scale = Fonts.SMALL;
        int maxW = Math.max(0, w - 4);
        String display = Fonts.ellipsize(label, maxW, scale);
        int textH = Fonts.height(scale);
        Fonts.drawCentered(ctx, display, x + w / 2, y + (h - textH) / 2, textC, scale);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) return false;
        boolean inside = contains(mx, my);
        if (listening) {
            if (inside) {
                if (button != 0) {
                    binding.setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
                    KeyBinding.updateKeysByCode();
                    MinecraftClient.getInstance().options.write();
                }
                listening = false;
                IslesClient.playMenuClickSound();
                return true;
            }
            listening = false;
            return false;
        }
        if (button != 0 || !inside) return false;
        listening = true;
        IslesClient.playMenuClickSound();
        return true;
    }

    @Override public boolean keyPressed(KeyInput in) {
        if (!listening) return false;
        if (in.key() == GLFW.GLFW_KEY_ESCAPE) {
            binding.setBoundKey(InputUtil.UNKNOWN_KEY);
        } else {
            binding.setBoundKey(InputUtil.fromKeyCode(in));
        }
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
        listening = false;
        return true;
    }

    @Override public void unfocus() { listening = false; }
}
