package com.islesplus.screen.islesscreen;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.CheckChip;
import com.islesplus.ui.widgets.Label;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

class RiftWarningScreen extends Screen {
    private static final int DIALOG_W = 300;
    private static final int PAD = 7;

    private static final String TITLE = "Heads up";
    private static final String BODY =
        "Rift features provide gameplay assistance that may affect your " +
        "experience. The Rift is designed to be discovered naturally. " +
        "We recommend experiencing it unaided before enabling these.";

    private final Screen parent;
    private final Runnable onConfirm;
    private final Flow.Column body;
    private boolean dontShowAgain = false;

    private int panelX, panelY, panelW, panelH;

    RiftWarningScreen(Screen parent, Runnable onConfirm) {
        super(Text.empty());
        this.parent = parent;
        this.onConfirm = onConfirm;
        this.body = new Flow.Column(9)
            .add(new Label(BODY, Theme.TEXT_LABEL, Fonts.TITLE).wrap())
            .add(new CheckChip("Don't show this again", () -> dontShowAgain, () -> dontShowAgain = !dontShowAgain))
            .add(new Flow.WrapRow(6, 4)
                .add(new Button("Cancel", Button.Kind.QUIET, () -> MinecraftClient.getInstance().setScreen(parent)).fill())
                .add(new Button("Enable", Button.Kind.PRIMARY, () -> {
                    if (dontShowAgain) RiftWarningManager.setDismissed();
                    onConfirm.run();
                    MinecraftClient.getInstance().setScreen(parent);
                }).fill()));
    }

    private void initUi() {
        super.init();
        Fonts.resetFallbackCheck();
    }

    /** Idempotent: recomputes the panel bounds and lays out the body Column inside it. Called
     * every frame from render() and again before hit-testing in mouseClicked(). */
    private void layoutPanel() {
        panelW = Math.min(DIALOG_W, width - 20);
        int bodyWidth = Math.max(0, panelW - 2 * PAD);

        // Measuring pass: layout() must be idempotent, so a placeholder position is safe here.
        int bodyH = body.layout(0, 0, bodyWidth);
        panelH = Metrics.DIALOG_TITLE_H + bodyH + 2 * PAD;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        // Final pass at the real position.
        int bodyX = panelX + PAD, bodyY = panelY + Metrics.DIALOG_TITLE_H + PAD;
        body.layout(bodyX, bodyY, bodyWidth);
    }

    private void renderUi(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, Theme.SCRIM);
        layoutPanel();

        ctx.fill(panelX, panelY + 5, panelX + panelW, panelY + panelH + 5, Theme.DROP_SHADOW);
        Draw.bevel4(ctx, panelX, panelY, panelW, panelH,
            Theme.SURFACE, Theme.SURFACE_LIT, Theme.SURFACE_SHADE, Theme.SURFACE_LIT_SIDE, Theme.SURFACE_SHADE_SIDE, Theme.INK);
        Draw.ring(ctx, panelX - 1, panelY - 1, panelW + 2, panelH + 2, Theme.INK);

        Draw.bevel(ctx, panelX, panelY, panelW, Metrics.DIALOG_TITLE_H,
            Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);

        int textH = Fonts.height(Fonts.BODY);
        Fonts.drawHeading(ctx, TITLE, panelX + (panelW - Fonts.headingWidth(TITLE, Fonts.BODY)) / 2, panelY + (Metrics.DIALOG_TITLE_H - textH) / 2, Theme.CREAM, Fonts.BODY);

        body.render(ctx, mouseX, mouseY);
    }

    private boolean mouseClickedUi(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        layoutPanel();
        body.mouseClicked(click.x(), click.y(), click.button());
        return true;
    }

    private boolean mouseReleasedUi(Click click) {
        body.mouseReleased();
        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    // /ip UI: its text is in the Isles+ faces (Fonts.islesUi), everything else's in the game's font.
    @Override protected void init() { Fonts.islesUi(this::initUi); }
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) { Fonts.islesUi(() -> renderUi(ctx, mouseX, mouseY, delta)); }
    @Override public boolean mouseClicked(Click click, boolean doubled) { return Fonts.islesUi(() -> mouseClickedUi(click, doubled)); }
    @Override public boolean mouseReleased(Click click) { return Fonts.islesUi(() -> mouseReleasedUi(click)); }
}
