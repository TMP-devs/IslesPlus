package com.islesplus.features.bosstracker;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public final class BossTrackerCardRenderer {

    private static final int PANEL_SOFT  = 0xEE262626;
    private static final int PANEL_HOVER = 0xEE313131;
    private static final int BORDER      = 0xFF464646;
    private static final int TEXT_PRIMARY = 0xFFF0F0F0;
    private static final int TEXT_MUTED   = 0xFFB3B3B3;
    private static final int POSITIVE     = 0xFF38D286;
    private static final int BETA_RED     = 0xFFFF5C5C;
    private static final float BETA_SCALE = 0.75f;

    private BossTrackerCardRenderer() {}

    public static int getExpandedHeight() {
        int h = 4; // top pad
        h += 14;   // auto /bossary row
        h += 6;    // gap
        h += 14;   // position row
        h += 6;    // gap
        h += 1;    // separator
        h += 6;    // gap after separator
        List<BossTracker.TrackedBoss> bosses = BossTracker.bosses;
        if (bosses.isEmpty()) {
            h += 10; // "Open /bossary" message
        } else {
            h += bosses.size() * 14;
        }
        h += 4 + 1 + 4 + 9 + 10; // separator + key lines
        h += 4; // bottom pad
        return h;
    }

    public static void draw(DrawContext ctx, TextRenderer tr, int x, int y, int width, int mouseX, int mouseY) {
        int curY = y + 4;

        // Row 1: Auto /bossary toggle
        boolean autoHov = isInside(mouseX, mouseY, x, curY, width, 14);
        if (autoHov) ctx.fill(x, curY, x + width, curY + 14, PANEL_HOVER);
        drawAutoBossaryToggle(ctx, tr, x + 4, curY + 3, BossTracker.autoOpenBossary);
        curY += 14 + 6;

        // Row 2: HUD position options
        String[] posLabels = {"Top L", "Top R", "Bot L", "Bot R"};
        BossTracker.BossHudPosition[] positions = {
            BossTracker.BossHudPosition.TOP_LEFT,
            BossTracker.BossHudPosition.TOP_RIGHT,
            BossTracker.BossHudPosition.BOTTOM_LEFT,
            BossTracker.BossHudPosition.BOTTOM_RIGHT
        };
        int posW = width / 4;
        for (int i = 0; i < 4; i++) {
            boolean sel = BossTracker.hudPosition == positions[i];
            boolean hov = isInside(mouseX, mouseY, x + posW * i, curY, posW - 1, 12);
            ctx.fill(x + posW * i, curY, x + posW * i + posW - 1, curY + 12, hov ? PANEL_HOVER : PANEL_SOFT);
            ctx.fill(x + posW * i, curY, x + posW * i + posW - 1, curY + 1, sel ? POSITIVE : BORDER);
            String lbl = posLabels[i];
            ctx.drawText(tr, Text.literal(lbl),
                x + posW * i + (posW - tr.getWidth(lbl)) / 2, curY + 2,
                sel ? TEXT_PRIMARY : TEXT_MUTED, false);
        }
        curY += 14 + 6;

        // Separator
        ctx.fill(x, curY, x + width, curY + 1, BORDER);
        curY += 1 + 6;

        // Boss list
        List<BossTracker.TrackedBoss> bosses = BossTracker.bosses;
        if (bosses.isEmpty()) {
            ctx.drawText(tr, Text.literal("Open /bossary to load bosses"), x + 4, curY + 1, TEXT_MUTED, false);
            curY += 10;
        } else {
            for (int i = 0; i < bosses.size(); i++) {
                BossTracker.TrackedBoss boss = bosses.get(i);
                boolean hidden = BossTracker.hiddenBossNames.contains(boss.name);
                boolean rowHov = isInside(mouseX, mouseY, x, curY, width, 12);
                if (rowHov) ctx.fill(x, curY, x + width, curY + 12, PANEL_HOVER);
                drawMiniToggle(ctx, tr, x + 4, curY + 2, boss.name, !hidden);
                curY += 14;
            }
        }

        // Key
        curY += 4;
        ctx.fill(x, curY, x + width, curY + 1, BORDER);
        curY += 1 + 4;
        ctx.drawText(tr, Text.literal("~    estimated timer"), x + 4, curY, 0xFF555555, false);
        curY += 10;
        ctx.drawText(tr, Text.literal("/bossary to refresh or proximity to boss"), x + 4, curY, 0xFF555555, false);
    }

    public static boolean onClick(int mouseX, int mouseY, int x, int y, int width) {
        int curY = y + 4;

        // Auto /bossary toggle
        if (isInside(mouseX, mouseY, x, curY, width, 14)) {
            BossTracker.autoOpenBossary = !BossTracker.autoOpenBossary;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }
        curY += 14 + 6;

        // Position options
        BossTracker.BossHudPosition[] positions = {
            BossTracker.BossHudPosition.TOP_LEFT,
            BossTracker.BossHudPosition.TOP_RIGHT,
            BossTracker.BossHudPosition.BOTTOM_LEFT,
            BossTracker.BossHudPosition.BOTTOM_RIGHT
        };
        int posW = width / 4;
        if (isInside(mouseX, mouseY, x, curY, width, 12)) {
            for (int i = 0; i < 4; i++) {
                if (isInside(mouseX, mouseY, x + posW * i, curY, posW - 1, 12)) {
                    BossTracker.hudPosition = positions[i];
                    com.islesplus.IslesPlusConfig.save();
                    return true;
                }
            }
        }
        curY += 14 + 6;

        // Separator
        curY += 1 + 6;

        // Boss list toggles
        List<BossTracker.TrackedBoss> bosses = BossTracker.bosses;
        for (int i = 0; i < bosses.size(); i++) {
            if (isInside(mouseX, mouseY, x, curY, width, 12)) {
                String name = bosses.get(i).name;
                if (BossTracker.hiddenBossNames.contains(name)) {
                    BossTracker.hiddenBossNames.remove(name);
                } else {
                    BossTracker.hiddenBossNames.add(name);
                }
                com.islesplus.IslesPlusConfig.save();
                return true;
            }
            curY += 14;
        }

        return true;
    }

    private static void drawMiniToggle(DrawContext ctx, TextRenderer tr, int x, int y, String label, boolean enabled) {
        int w = 24, h = 10;
        ctx.fill(x, y, x + w, y + h, enabled ? POSITIVE : 0xFF5B6678);
        int knobW = h - 4;
        int knobX = enabled ? x + w - knobW - 2 : x + 2;
        ctx.fill(knobX, y + 2, knobX + knobW, y + 2 + knobW, 0xFFFFFFFF);
        ctx.drawText(tr, Text.literal(label), x + w + 4, y + 1, enabled ? TEXT_PRIMARY : TEXT_MUTED, false);
    }

    private static void drawAutoBossaryToggle(DrawContext ctx, TextRenderer tr, int x, int y, boolean enabled) {
        int w = 24, h = 10;
        ctx.fill(x, y, x + w, y + h, enabled ? POSITIVE : 0xFF5B6678);
        int knobW = h - 4;
        int knobX = enabled ? x + w - knobW - 2 : x + 2;
        ctx.fill(knobX, y + 2, knobX + knobW, y + 2 + knobW, 0xFFFFFFFF);

        int labelX = x + w + 4;
        int labelY = y + 1;
        String label = "Auto /bossary on join";
        int labelColor = enabled ? TEXT_PRIMARY : TEXT_MUTED;
        ctx.drawText(tr, Text.literal(label), labelX, labelY, labelColor, false);

        int betaBaseX = labelX + tr.getWidth(label) + 4;
        int betaBaseY = y + 2;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(BETA_SCALE, BETA_SCALE);
        int betaX = Math.round(betaBaseX / BETA_SCALE);
        int betaY = Math.round(betaBaseY / BETA_SCALE);
        ctx.drawText(tr, Text.literal("(beta)"), betaX, betaY, BETA_RED, false);
        ctx.getMatrices().popMatrix();
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
