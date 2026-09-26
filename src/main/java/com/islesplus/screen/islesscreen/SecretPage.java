package com.islesplus.screen.islesscreen;

import com.islesplus.features.secret.FoundReport;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.NoteTooltip;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The page behind the Konami code. A second scroll, the size of the /ip one, slowly unrolls over
 * it; the message is hand-written in the signature script, and "I saw nothing" at the foot rolls
 * it away again. Nothing else is here yet - which is what the message says.
 */
final class SecretPage extends Widget {
    private static final String MESSAGE =
        "Oh, this is embarrassing... you weren't supposed to find this yet. Check back in a future update.";
    /** Opaque enough that the /ip scroll underneath does not show while this one is still rolled up. */
    private static final int BACKDROP = 0xF20D0B09;
    private static final int UNROLL_STEPS = 90, UNROLL_MS_PER_STEP = 20;   // about 1.8 s
    /** The script's ink rises ~10 px above the y it is drawn at and falls ~13 px below it. */
    private static final int LINE_PITCH = 28, INK_RISE = 10;
    private static final int TEXT_INSET = 24, BUTTON_W = 130, BUTTON_BOTTOM_MARGIN = 6;
    private static final int BADGE = 9, BADGE_GAP = 5;
    /** Shown when the pointer is over the "i" beside the button: players are told before they click. */
    private static final String REPORT_NOTE = "Clicking this lets the Isles+ team know you found this page: "
        + "your Minecraft username is sent to us, once. Press Escape to leave without sending anything.";

    private final OverlayHost host;
    private final int wantedContentW;
    private final ScrollFrame frame = new ScrollFrame(UNROLL_STEPS, UNROLL_MS_PER_STEP);
    private final Button back;
    private List<String> lines = List.of();
    private int wrappedForW = -1;

    private SecretPage(OverlayHost host, int wantedContentW) {
        this.host = host;
        this.wantedContentW = wantedContentW;
        this.back = new Button("I saw nothing", Button.Kind.PRIMARY, () -> {
            FoundReport.sendOnce();   // verified with Mojang, once per player; see the note beside the button
            host.closeOverlay(this);
        }).fill();
    }

    /** @param wantedContentW the /ip scroll's content width, so this scroll covers it exactly */
    static void open(OverlayHost host, int wantedContentW) {
        host.openOverlay(new SecretPage(host, wantedContentW));
    }

    @Override public int layout(int x, int y, int width) {
        int screenW = host.screenWidth(), screenH = host.screenHeight();
        this.x = 0; this.y = 0; this.w = screenW; this.h = screenH;
        frame.layout(MinecraftClient.getInstance(), screenW, screenH, wantedContentW);

        int wrapW = Math.max(40, frame.contentW - 2 * TEXT_INSET);
        if (wrapW != wrappedForW) {   // layout runs every frame; the wrap only changes with the width
            lines = wrapScript(MESSAGE, wrapW);
            wrappedForW = wrapW;
        }
        int buttonW = Math.min(BUTTON_W, frame.contentW);
        back.layout(frame.contentX + (frame.contentW - buttonW) / 2,
            frame.contentY + frame.contentH - Metrics.BUTTON_H - BUTTON_BOTTOM_MARGIN, buttonW);
        return this.h;
    }

    /** Greedy word wrap measured in the script font (Fonts.wrap measures the body face). */
    private static List<String> wrapScript(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && Fonts.scriptWidth(trial) > maxWidth) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(trial);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(0, 0, w, h, BACKDROP);
        frame.render(ctx);

        int revealBottom = frame.revealBottom();
        if (revealBottom <= frame.contentY) return;
        // Everything on the sheet appears only as far as the paper has unrolled.
        ctx.enableScissor(frame.contentX - 1, frame.contentY - 1, frame.contentX + frame.contentW + 1, revealBottom);

        int blockH = lines.size() * LINE_PITCH;
        int areaH = back.y - frame.contentY;                       // the space above the button
        int firstY = frame.contentY + Math.max(INK_RISE + 4, (areaH - blockH) / 2 + INK_RISE);
        boolean script = Fonts.scriptAvailable();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineY = firstY + i * LINE_PITCH;
            if (script) {
                Fonts.drawScript(ctx, line, frame.contentX + (frame.contentW - Fonts.scriptWidth(line)) / 2, lineY, Theme.TEXT_TITLE);
            } else {
                Fonts.drawCentered(ctx, line, frame.contentX + frame.contentW / 2, lineY, Theme.TEXT_TITLE, Fonts.BODY);
            }
        }
        back.render(ctx, mouseX, mouseY);

        // The same oxblood "i" badge the card headers use, just right of the button.
        int bx = back.x + back.w + BADGE_GAP, by = back.y + (back.h - BADGE) / 2;
        Draw.bevel(ctx, bx, by, BADGE, BADGE, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);
        int stemX = bx + BADGE / 2;
        ctx.fill(stemX, by + 2, stemX + 1, by + 3, Theme.CREAM);
        ctx.fill(stemX, by + 4, stemX + 1, by + BADGE - 2, Theme.CREAM);
        ctx.disableScissor();

        boolean overBadge = mouseX >= bx - 1 && mouseX < bx + BADGE + 1 && mouseY >= by - 1 && mouseY < by + BADGE + 1;
        if (overBadge && by + BADGE <= revealBottom) NoteTooltip.draw(ctx, REPORT_NOTE, mouseX, mouseY, w, h);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        // Only once the paper has reached the button; the page swallows every other click.
        if (my < frame.revealBottom()) back.mouseClicked(mx, my, button);
        return true;
    }

    @Override public void mouseReleased() { back.mouseReleased(); }
}
