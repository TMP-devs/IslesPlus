package com.islesplus.screen.hudedit;

import com.islesplus.IslesClient;
import com.islesplus.IslesPlusConfig;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudElements;
import com.islesplus.hud.HudLayout;
import com.islesplus.hud.HudPlacement;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.InfoChip;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.Panel;
import com.islesplus.ui.widgets.SmallToggle;
import com.islesplus.ui.widgets.ValueSlider;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The HUD editor: every Isles+ element that shows on the player's screen, drawn with sample
 * content where there is nothing live, over the game. No toolbar: clicking an element selects it
 * and opens a small card beside it (its size, RESET, and for the rift rank STICK TO SCOREBOARD).
 * Drag to move; scroll over it, or drag its corner handle, to scale it. Arrow keys nudge the
 * selected one and Tab selects the next, for whoever finds them. An element snaps to the middle of
 * the screen on either axis. Esc saves and leaves.
 */
public class HudEditScreen extends Screen {
    private static final int HANDLE = 6, SNAP = 4, TAG_GAP = 2, CARD_W = 150, CARD_GAP = 5, EDGE = 4;
    private static final float WHEEL_STEP = 0.1f;
    private static final String IDLE_HINT = "Click something to edit it - Esc when done";
    /** Vanilla sidebar geometry: 9 px rows, the title row plus a 1 px gap above the list. */
    private static final int SIDEBAR_ROW_H = 9;
    private static final String MOCK_TITLE = "SCOREBOARD";
    private static final String[] MOCK_LINES = {"Example line", "Another line", "Score: 1,234", "Time: 12:34", "Players: 4"};

    private enum Drag { NONE, MOVE, RESIZE }

    private final Screen parent;
    /** The card for the selected element. */
    private Widget card;
    private SmallToggle stickToggle;
    private Label hint;
    /** Background opacity, only for an element that has a background (World Bosses). */
    private Widget opacityRow;

    private HudElement selected;
    private Drag drag = Drag.NONE;
    private double dragMouseX, dragMouseY;
    private HudLayout.Box dragStart;
    private boolean moved, snapX, snapY;

    /** @param parent screen to go back to when done, or null for the game. */
    public HudEditScreen(Screen parent) {
        super(Text.literal("Isles+ HUD Editor"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        Fonts.islesUi(this::initUi);
    }

    private void initUi() {
        Fonts.resetFallbackCheck();
        stickToggle = new SmallToggle("Stick to scoreboard",
            () -> selected != null && HudLayout.stuck(selected),
            () -> {
                if (selected == null) return;
                HudLayout.setStuck(selected, !HudLayout.stuck(selected), this.width, this.height);
                IslesPlusConfig.save();
            });
        hint = new Label(this::hintText, Theme.TEXT_META, Fonts.SMALL).wrap();
        Widget body = new Flow.Column(Metrics.DRAWER_GAP)
            .add(new Flow.WrapRow(4, 4)
                .add(new Label(() -> selected == null ? "" : selected.name, Theme.TEXT_TITLE, Fonts.BODY).wrap())
                .add(new InfoChip(() -> selected == null ? "" : Math.round(HudLayout.placement(selected).scale * 100) + "%")))
            .add(stickToggle)
            .add(opacityRow = new Flow.WrapRow(4, 4)
                .add(new Label("Background", Theme.TEXT_LABEL, Fonts.SMALL))
                .add(new ValueSlider(
                    () -> selected == null ? 1f : HudLayout.placement(selected).opacity,
                    v -> {
                        if (selected == null) return;
                        HudPlacement p = HudLayout.placement(selected).copy();
                        p.opacity = HudPlacement.clampOpacity(v);
                        HudLayout.set(selected, p);
                    },
                    0f, 1f, 0.05f, IslesPlusConfig::save))
                .add(new InfoChip(() -> selected == null ? "" : Math.round(HudLayout.placement(selected).opacity * 100) + "%")
                    .fixed(Fonts.width("100%", Fonts.SMALL) + 2 * Metrics.CHIP_PAD_X)))
            .add(hint)
            .add(new Flow.WrapRow(4, 4).align(Flow.Align.END)
                .add(new Button("Reset", Button.Kind.SECONDARY, this::resetSelected).small()
                    .disabledWhen(() -> selected == null || !HudLayout.isCustom(selected))));
        card = new Panel(body, 6, Theme.SURFACE, Theme.SURFACE_LIT, Theme.SURFACE_SHADE, Theme.SURFACE_RING);
    }

    /** Only what is not obvious: how to resize, and anything special about this element. */
    private String hintText() {
        if (selected == null) return "";
        StringBuilder sb = new StringBuilder();
        if (!selected.enabled()) sb.append("This feature is off. ");
        if (selected.editorHint() != null) sb.append(selected.editorHint()).append(". ");
        if (HudLayout.stuck(selected)) sb.append("Stuck to the scoreboard: only its size changes. ");
        sb.append("Scroll or drag the corner to resize.");
        return sb.toString();
    }

    // ==============================
    // Geometry
    // ==============================

    /** The vanilla sidebar's spot for our sample lines (same maths as InGameHudScoreboardMixin). */
    private int[] mockScoreboardBounds() {
        TextRenderer tr = this.textRenderer;
        int maxW = tr.getWidth(MOCK_TITLE);
        for (String s : MOCK_LINES) maxW = Math.max(maxW, tr.getWidth(s));
        int listH = MOCK_LINES.length * SIDEBAR_ROW_H;
        int bottom = this.height / 2 + listH / 3;
        int top = bottom - listH - SIDEBAR_ROW_H - 1;
        return new int[]{this.width - maxW - 3, top, maxW + 3, bottom - top};
    }

    private HudLayout.Box box(HudElement e) {
        return HudLayout.place(e, this.width, this.height, true);
    }

    private static boolean onHandle(HudLayout.Box b, double mx, double my) {
        int hx = b.x() + b.w() - HANDLE / 2, hy = b.y() + b.h() - HANDLE / 2;
        return mx >= hx && mx < hx + HANDLE && my >= hy && my < hy + HANDLE;
    }

    /** Topmost element under the point (the last drawn wins), or null. */
    private HudElement elementAt(double mx, double my) {
        List<HudElement> all = HudElements.all();
        // The selected element's handle sticks out past its box: it wins first.
        if (selected != null) {
            HudLayout.Box b = box(selected);
            if (b != null && onHandle(b, mx, my)) return selected;
        }
        for (int i = all.size() - 1; i >= 0; i--) {
            HudLayout.Box b = box(all.get(i));
            if (b != null && b.contains(mx, my)) return all.get(i);
        }
        return null;
    }

    /** Lays the card out beside the selected element: under it, else over it, else wherever it fits
     * on screen. False when there is no card to show (nothing selected, or mid-drag). */
    private boolean layoutCard() {
        return Fonts.islesUi(this::layoutCardUi);
    }

    private boolean layoutCardUi() {
        if (selected == null || drag != Drag.NONE) return false;
        HudLayout.Box b = box(selected);
        if (b == null) return false;
        stickToggle.visible = selected.scoreboardAttached();
        opacityRow.visible = selected.hasBackgroundOpacity();
        int w = Math.min(CARD_W, this.width - 2 * EDGE);
        int h = card.layout(0, 0, w);
        int x = HudAnchor.clamp(b.x(), this.width - EDGE, w);
        int below = b.y() + b.h() + HANDLE / 2 + CARD_GAP;
        int above = b.y() - CARD_GAP - h;
        int y = below + h <= this.height - EDGE ? below : above >= EDGE ? above : HudAnchor.clamp(below, this.height - EDGE, h);
        card.layout(Math.max(EDGE, x), Math.max(EDGE, y), w);
        return true;
    }

    // ==============================
    // Render
    // ==============================

    /** A light tint only: the point is to see the elements over the real game. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, Theme.HUD_EDIT_SCRIM);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        HudLayout.mockScoreboard = ScoreboardTracker.valid ? null : mockScoreboardBounds();
        if (HudLayout.mockScoreboard != null) drawMockScoreboard(ctx, HudLayout.mockScoreboard);

        boolean showCard = layoutCard();
        boolean overCard = showCard && card.contains(mouseX, mouseY);
        HudElement hovered = drag != Drag.NONE || overCard ? null : elementAt(mouseX, mouseY);
        for (HudElement e : HudElements.all()) {
            HudLayout.Box b = box(e);
            if (b == null) continue;
            ctx.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), Theme.HUD_EDIT_FILL);
            HudLayout.draw(ctx, e, b, this.width, this.height, true);
        }

        // Outlines, tags and handles over every element, so a neighbour never hides them.
        ctx.createNewRootLayer();
        for (HudElement e : HudElements.all()) {
            HudLayout.Box b = box(e);
            if (b == null) continue;
            boolean active = e == selected || e == hovered;
            Draw.ring(ctx, b.x(), b.y(), b.w(), b.h(), active ? Theme.HUD_ALERT : Theme.HUD_EDIT_RING);
            if (e == hovered && e != selected) drawTag(ctx, e, b);
            if (e == selected) {
                int hx = b.x() + b.w() - HANDLE / 2, hy = b.y() + b.h() - HANDLE / 2;
                Draw.bevel(ctx, hx, hy, HANDLE, HANDLE, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);
            }
        }

        if (drag == Drag.MOVE) {
            if (snapX) ctx.fill(this.width / 2, 0, this.width / 2 + 1, this.height, Theme.HUD_ALERT);
            if (snapY) ctx.fill(0, this.height / 2, this.width, this.height / 2 + 1, Theme.HUD_ALERT);
        }
        if (showCard) {
            ctx.createNewRootLayer();
            Fonts.islesUi(() -> card.render(ctx, mouseX, mouseY));
        } else if (selected == null) {
            Fonts.drawHud(ctx, IDLE_HINT, (this.width - Fonts.hudWidth(IDLE_HINT, Fonts.SMALL)) / 2, EDGE, Theme.HUD_TEXT, Fonts.SMALL);
        }
    }

    /** The name over a hovered element (under it at the top of the screen). */
    private void drawTag(DrawContext ctx, HudElement e, HudLayout.Box b) {
        String name = e.name + (e.enabled() ? "" : " (off)");
        int y = b.y() - TAG_GAP - Fonts.height(Fonts.SMALL) - 1;
        if (y < 0) y = b.y() + b.h() + TAG_GAP + 1;
        int x = Math.max(1, Math.min(b.x(), this.width - 1 - Fonts.hudWidth(name, Fonts.SMALL)));
        Fonts.drawHud(ctx, name, x, y, e.enabled() ? Theme.HUD_TEXT : Theme.HUD_MUTED, Fonts.SMALL);
    }

    /** Stand-in for the vanilla sidebar (its own look: plain font, dark translucent rows). */
    private void drawMockScoreboard(DrawContext ctx, int[] sb) {
        int x = sb[0], y = sb[1], right = sb[0] + sb[2] - 1, bottom = sb[1] + sb[3];
        ctx.fill(x - 2, y, right, y + SIDEBAR_ROW_H, Theme.VANILLA_SIDEBAR_TITLE_BG);
        ctx.fill(x - 2, y + SIDEBAR_ROW_H, right, bottom, Theme.VANILLA_SIDEBAR_BG);
        ctx.drawText(this.textRenderer, MOCK_TITLE, x + (sb[2] - 3 - this.textRenderer.getWidth(MOCK_TITLE)) / 2, y + 1, Theme.HUD_TEXT, false);
        int rowY = y + SIDEBAR_ROW_H + 1;
        for (String s : MOCK_LINES) {
            ctx.drawText(this.textRenderer, s, x, rowY, Theme.HUD_MUTED, false);
            rowY += SIDEBAR_ROW_H;
        }
    }

    // ==============================
    // Editing
    // ==============================

    /** Puts the element's top-left at (x, y) at this scale and keeps it there. */
    private void apply(HudElement e, int x, int y, float scale) {
        HudElement.Size base = e.measure(true);
        int w = HudAnchor.scaled(base.w(), scale), h = HudAnchor.scaled(base.h(), scale);
        boolean stuck = HudLayout.stuck(e);
        if (!stuck) {
            x = HudAnchor.clamp(x, this.width, w);
            y = HudAnchor.clamp(y, this.height, h);
        }
        HudLayout.set(e, HudLayout.placementAt(e, !stuck, x, y, w, h, scale, this.width, this.height));
    }

    /** New scale, keeping the element's centre (or, on the scoreboard, its bottom centre) still. */
    private void rescale(HudElement e, float scale) {
        HudLayout.Box b = box(e);
        if (b == null) return;
        scale = HudPlacement.clampScale(scale);
        HudElement.Size base = e.measure(true);
        int w = HudAnchor.scaled(base.w(), scale), h = HudAnchor.scaled(base.h(), scale);
        int x = b.x() + b.w() / 2 - w / 2;
        int y = HudLayout.stuck(e) ? b.y() + b.h() - h : b.y() + b.h() / 2 - h / 2;
        apply(e, x, y, scale);
    }

    private void resetSelected() {
        if (selected == null) return;
        HudLayout.reset(selected);
        IslesPlusConfig.save();
    }

    // ==============================
    // Input
    // ==============================

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return true;
        double mx = click.x(), my = click.y();
        if (layoutCard() && card.contains(mx, my)) {
            Fonts.islesUi(() -> card.mouseClicked(mx, my, 0));
            return true;
        }
        HudElement hit = elementAt(mx, my);
        HudLayout.Box b = hit == null ? null : box(hit);
        // The handle is only there (drawn) on the element that was already selected.
        boolean handle = hit != null && hit == selected && onHandle(b, mx, my);
        selected = hit;
        if (hit == null) return true;
        drag = handle ? Drag.RESIZE : Drag.MOVE;
        dragMouseX = mx;
        dragMouseY = my;
        dragStart = b;
        moved = false;
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (drag == Drag.NONE && Fonts.islesUi(() -> card.mouseDragged(click.x(), click.y()))) return true;   // the opacity slider
        if (drag == Drag.NONE || selected == null) return true;
        double dx = click.x() - dragMouseX, dy = click.y() - dragMouseY;
        if (!moved && Math.abs(dx) < 1 && Math.abs(dy) < 1) return true;
        moved = true;
        if (drag == Drag.MOVE) {
            if (HudLayout.stuck(selected)) return true;   // locked on the scoreboard: only its size changes
            int x = dragStart.x() + (int) Math.round(dx);
            int y = dragStart.y() + (int) Math.round(dy);
            snapX = Math.abs(x + dragStart.w() / 2.0 - this.width / 2.0) <= SNAP;
            snapY = Math.abs(y + dragStart.h() / 2.0 - this.height / 2.0) <= SNAP;
            if (snapX) x = (this.width - dragStart.w()) / 2;
            if (snapY) y = (this.height - dragStart.h()) / 2;
            apply(selected, x, y, dragStart.scale());
        } else {
            // Grow with the corner: how far it moved, against the element's size, top-left fixed.
            double grow = (dx + dy) / (double) Math.max(1, dragStart.w() + dragStart.h());
            float scale = HudPlacement.clampScale((float) (dragStart.scale() * (1 + grow)));
            apply(selected, dragStart.x(), dragStart.y(), scale);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        card.mouseReleased();
        if (drag != Drag.NONE && moved) IslesPlusConfig.save();
        drag = Drag.NONE;
        snapX = snapY = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (drag != Drag.NONE || verticalAmount == 0) return true;
        if (layoutCard() && card.contains(mouseX, mouseY)) return true;
        HudElement e = elementAt(mouseX, mouseY);
        if (e == null) return true;
        selected = e;
        rescale(e, HudLayout.placement(e).scale + (verticalAmount > 0 ? WHEEL_STEP : -WHEEL_STEP));
        IslesPlusConfig.save();
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_TAB && drag == Drag.NONE) {
            List<HudElement> all = HudElements.all();
            int back = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? -1 : 1;
            int i = selected == null ? (back > 0 ? -1 : 0) : all.indexOf(selected);
            selected = all.get(Math.floorMod(i + back, all.size()));
            IslesClient.playMenuClickSound();
            return true;
        }
        if (selected != null && drag == Drag.NONE) {
            if (HudLayout.stuck(selected)) return super.keyPressed(input);
            int step = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            int dx = 0, dy = 0;
            switch (input.key()) {
                case GLFW.GLFW_KEY_LEFT -> dx = -step;
                case GLFW.GLFW_KEY_RIGHT -> dx = step;
                case GLFW.GLFW_KEY_UP -> dy = -step;
                case GLFW.GLFW_KEY_DOWN -> dy = step;
                default -> { return super.keyPressed(input); }
            }
            HudLayout.Box b = box(selected);
            if (b != null) {
                apply(selected, b.x() + dx, b.y() + dy, b.scale());
                IslesPlusConfig.save();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void removed() {
        HudLayout.mockScoreboard = null;
        IslesPlusConfig.save();
        super.removed();
    }

    @Override
    public void close() {
        HudLayout.mockScoreboard = null;
        if (this.client != null) this.client.setScreen(parent);
    }
}
