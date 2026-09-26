package com.islesplus.ui.widgets;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

/** Modal overlay: full-screen scrim behind a centred, titled panel that hosts a body widget.
 * Positions and lays out itself from {@link OverlayHost#screenWidth()}/{@code screenHeight()}
 * each frame; forwards all input to the title-bar close button and the body. */
public class Dialog extends Widget {
    private static final int PAD = 7;

    private final OverlayHost host;
    private final String title;
    private final Widget body;
    private final Runnable onClose;
    private final Button closeButton;

    private int panelX, panelY, panelW, panelH;
    private int maxWidth = Metrics.DIALOG_W;

    public Dialog(OverlayHost host, String title, Widget body, Runnable onClose) {
        this.host = host;
        this.title = title;
        this.body = body;
        this.onClose = onClose;
        this.closeButton = new Button("", Button.Kind.ICON, this::close);
    }

    /** A wider card than the default {@link Metrics#DIALOG_W}, for dialogs that are mostly reading. */
    public Dialog width(int maxWidth) { this.maxWidth = maxWidth; return this; }

    private void close() {
        host.closeOverlay(this);
        if (onClose != null) onClose.run();
    }

    @Override public int layout(int x, int y, int width) {
        int screenW = host.screenWidth(), screenH = host.screenHeight();
        this.x = 0; this.y = 0; this.w = screenW; this.h = screenH;

        panelW = Math.min(maxWidth, screenW - 20);
        int bodyWidth = Math.max(0, panelW - 2 * PAD);

        // Measuring pass: layout() must be idempotent, so a placeholder position is safe here.
        int bodyH = body.layout(0, 0, bodyWidth);
        panelH = Metrics.DIALOG_TITLE_H + bodyH + 2 * PAD;
        panelX = (screenW - panelW) / 2;
        panelY = panelH > screenH ? 10 : (screenH - panelH) / 2;

        // Final pass at the real position.
        int bodyX = panelX + PAD, bodyY = panelY + Metrics.DIALOG_TITLE_H + PAD;
        body.layout(bodyX, bodyY, bodyWidth);

        int closeSize = Metrics.ICON_BUTTON;
        int closeX = panelX + panelW - closeSize - 1;
        int closeY = panelY + (Metrics.DIALOG_TITLE_H - closeSize) / 2;
        closeButton.layout(closeX, closeY, closeSize);

        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(0, 0, w, h, Theme.SCRIM);

        ctx.fill(panelX, panelY + 5, panelX + panelW, panelY + panelH + 5, Theme.DROP_SHADOW);
        Draw.bevel4(ctx, panelX, panelY, panelW, panelH,
            Theme.SURFACE, Theme.SURFACE_LIT, Theme.SURFACE_SHADE, Theme.SURFACE_LIT_SIDE, Theme.SURFACE_SHADE_SIDE, Theme.INK);
        Draw.ring(ctx, panelX - 1, panelY - 1, panelW + 2, panelH + 2, Theme.INK);

        Draw.bevel(ctx, panelX, panelY, panelW, Metrics.DIALOG_TITLE_H,
            Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);

        int titleLeft = panelX + PAD;
        int titleRight = closeButton.x - 4;
        String shown = Fonts.ellipsizeHeading(title, Math.max(0, titleRight - titleLeft), Fonts.BODY);
        int textH = Fonts.height(Fonts.BODY);
        Fonts.drawHeading(ctx, shown, titleLeft, panelY + (Metrics.DIALOG_TITLE_H - textH) / 2, Theme.CREAM, Fonts.BODY);

        closeButton.render(ctx, mouseX, mouseY);
        body.render(ctx, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (closeButton.mouseClicked(mx, my, button)) return true;
        body.mouseClicked(mx, my, button);
        return true;
    }

    @Override public boolean mouseDragged(double mx, double my) {
        if (closeButton.mouseDragged(mx, my)) return true;
        return body.mouseDragged(mx, my);
    }

    @Override public void mouseReleased() {
        closeButton.mouseReleased();
        body.mouseReleased();
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        if (closeButton.mouseScrolled(mx, my, amount)) return true;
        return body.mouseScrolled(mx, my, amount);
    }

    @Override public boolean keyPressed(KeyInput in) {
        if (body.keyPressed(in)) return true;
        if (closeButton.keyPressed(in)) return true;
        if (in.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return false;
    }

    @Override public boolean charTyped(CharInput in) {
        if (body.charTyped(in)) return true;
        return closeButton.charTyped(in);
    }

    @Override public void unfocus() {
        body.unfocus();
    }
}
