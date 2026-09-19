package com.islesplus.screen.islesscreen;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * The parchment panel behind the dashboard: two wooden rolls with a stretched paper sheet between
 * them, plus the one-shot "unroll" reveal. Owns the panel/sheet/content rectangles.
 *
 * <p>The paper is drawn first and runs from the middle of the top roll to the middle of the bottom
 * roll, so it reads as one sheet tucked underneath both rolls. The rolls are drawn at a fixed
 * height as three slices (end caps at the art's own aspect, middle stretched) so a wide panel does
 * not make them thick.
 */
final class ScrollFrame {
    private static final Identifier PAPER = Identifier.of("islesplus", "textures/gui/paper_sheet.png");
    private static final Identifier ROLL_TOP = Identifier.of("islesplus", "textures/gui/roll_top.png");
    private static final Identifier ROLL_BOTTOM = Identifier.of("islesplus", "textures/gui/roll_bottom.png");
    private static final int SHEET_TEX_W = 260, SHEET_TEX_H = 165, ROLL_TEX_W = 260, ROLL_TEX_H = 34;
    /** Source width of each roll end cap; the rest of the texture is the stretchable middle. */
    private static final int ROLL_CAP_TEX_W = 26;

    private static final int MIN_W = 380, MIN_H = 250;
    /** Side margin as a fraction of the panel width, so a wider scroll keeps the same paper-to-card ratio. */
    private static final float INSET_X_RATIO = 26f / 490f;
    private static final int ROLL_H = 40;
    /** How far the content area may run under the rolls' concave inner edge. */
    private static final int OVERLAP = 3;
    private static final int INSET_Y = 8;
    private static final int UNROLL_STEPS = 10, UNROLL_MS_PER_STEP = 15;

    private final int unrollSteps, unrollMsPerStep;

    /** The /ip scroll: snaps open in about 0.15 s. */
    ScrollFrame() { this(UNROLL_STEPS, UNROLL_MS_PER_STEP); }

    /** A scroll with its own unroll pace; more steps make a slow unroll smooth. */
    ScrollFrame(int unrollSteps, int unrollMsPerStep) {
        this.unrollSteps = Math.max(1, unrollSteps);
        this.unrollMsPerStep = Math.max(1, unrollMsPerStep);
    }

    /** Fixed at construction so a window resize does not replay the unroll. */
    private final long openedAt = Util.getMeasuringTimeMs();

    int panelX, panelY, panelW, panelH;
    int sheetX, sheetY, sheetW, sheetH;
    int contentX, contentY, contentW, contentH;

    private int rollH, rollCapW, overhangTop, overhangBottom;
    private boolean texturesPresent;
    /** Sampled once per frame (in {@link #render}) so the paper and the scissor always agree. */
    private int revealedH;

    /** @param wantedContentW width the card grid asks for (two cards wide enough for their titles). */
    void layout(MinecraftClient client, int screenW, int screenH, int wantedContentW) {
        int wantedPanelW = Math.round(wantedContentW / (1f - 2f * INSET_X_RATIO));
        panelW = Math.min(screenW - 20, Math.max(MIN_W, wantedPanelW));
        panelH = Math.min(screenH - 20, Math.max(MIN_H, screenH - 36));
        panelX = (screenW - panelW) / 2;
        panelY = (screenH - panelH) / 2;

        rollH = ROLL_H;
        float rollScale = rollH / (float) ROLL_TEX_H;
        rollCapW = Math.round(ROLL_CAP_TEX_W * rollScale);
        overhangTop = Math.round(4 * rollScale) + 2;
        overhangBottom = Math.round(6 * rollScale) + 2;

        // "sheet" is the open area between the two rolls; the paper itself extends under them.
        sheetX = panelX;
        sheetW = panelW;
        sheetY = panelY + rollH - OVERLAP;
        sheetH = Math.max(1, panelH - 2 * rollH + 2 * OVERLAP);

        int insetX = Math.round(panelW * INSET_X_RATIO);
        contentX = sheetX + insetX;
        contentW = Math.max(1, sheetW - 2 * insetX);
        contentY = sheetY + INSET_Y;
        contentH = Math.max(1, sheetH - 2 * INSET_Y);

        texturesPresent = client != null
            && client.getResourceManager().getResource(PAPER).isPresent()
            && client.getResourceManager().getResource(ROLL_TOP).isPresent()
            && client.getResourceManager().getResource(ROLL_BOTTOM).isPresent();

        sampleReveal();
    }

    /** Samples the clock once: how much of the sheet has unrolled so far, in pixels. */
    private void sampleReveal() {
        int steps = Math.min(unrollSteps, (int) ((Util.getMeasuringTimeMs() - openedAt) / unrollMsPerStep));
        revealedH = sheetH * Math.max(0, steps) / unrollSteps;
    }

    /** Bottom edge of the usable (revealed) content area; content must be scissored to it.
     * Reads the height sampled by the current frame's {@link #render}, never the clock. */
    int revealBottom() { return sheetY + revealedH - OVERLAP; }

    void render(DrawContext ctx) {
        sampleReveal();
        int bottomRollY = sheetY + revealedH - OVERLAP;
        // Paper spans roll-centre to roll-centre so it is visibly connected to, and under, both rolls.
        int paperTop = panelY + rollH / 2;
        int paperBottom = bottomRollY + rollH / 2;
        int fullPaperH = Math.max(1, (sheetY + sheetH - OVERLAP + rollH / 2) - paperTop);
        int paperH = paperBottom - paperTop;

        if (texturesPresent) {
            if (paperH > 0) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, PAPER,
                    sheetX, paperTop, 0, 0, sheetW, paperH,
                    SHEET_TEX_W, Math.max(1, Math.round(SHEET_TEX_H * (float) paperH / fullPaperH)),
                    SHEET_TEX_W, SHEET_TEX_H);
            }
            drawRoll(ctx, ROLL_TOP, panelX - overhangTop, panelY, panelW + 2 * overhangTop);
            drawRoll(ctx, ROLL_BOTTOM, panelX - overhangBottom, bottomRollY, panelW + 2 * overhangBottom);
        } else {
            if (paperH > 0) {
                ctx.fill(sheetX, paperTop, sheetX + sheetW, paperBottom, Theme.PARCHMENT);
                Draw.ring(ctx, sheetX, paperTop, sheetW, paperH, Theme.INK);
            }
            Draw.bevel(ctx, panelX, panelY, panelW, rollH,
                Theme.SECONDARY, Theme.SECONDARY_LIT, Theme.SECONDARY_SHADE, Theme.INK);
            Draw.bevel(ctx, panelX, bottomRollY, panelW, rollH,
                Theme.SECONDARY, Theme.SECONDARY_LIT, Theme.SECONDARY_SHADE, Theme.INK);
        }
    }

    /** Three-slice roll: both end caps keep the art's aspect, only the middle is stretched. */
    private void drawRoll(DrawContext ctx, Identifier tex, int x, int y, int w) {
        int cap = Math.min(rollCapW, w / 2);
        int midW = w - 2 * cap;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, cap, rollH,
            ROLL_CAP_TEX_W, ROLL_TEX_H, ROLL_TEX_W, ROLL_TEX_H);
        if (midW > 0) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + cap, y, ROLL_CAP_TEX_W, 0, midW, rollH,
                ROLL_TEX_W - 2 * ROLL_CAP_TEX_W, ROLL_TEX_H, ROLL_TEX_W, ROLL_TEX_H);
        }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + w - cap, y, ROLL_TEX_W - ROLL_CAP_TEX_W, 0, cap, rollH,
            ROLL_CAP_TEX_W, ROLL_TEX_H, ROLL_TEX_W, ROLL_TEX_H);
    }
}
