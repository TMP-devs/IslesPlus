package com.islesplus.screen.islesscreen;

import com.islesplus.IslesClient;
import com.islesplus.IslesPlusConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.islesplus.screen.islesscreen.ScreenColors.*;

public class IslesScreen extends Screen {
    private static final int SCREEN_MIN_WIDTH = 380;
    private static final int SCREEN_MIN_HEIGHT = 250;

    private final CardRegistry cards = new CardRegistry();
    private Tab activeTab = Tab.QOL;

    // Expandable card state
    private final Set<FeatureCard> expandedCards = new HashSet<>();
    private boolean sliderDragging = false;
    private int dragSliderTrackX, dragSliderTrackWidth;
    private Consumer<Float> activeSliderConsumer = null;

    private int guiX, guiY, guiWidth, guiHeight;
    private int tabsX, tabsY, tabsWidth, tabsHeight;
    private int contentX, contentY, contentWidth, contentHeight;

    public IslesScreen() {
        super(Text.literal("IslesPlus"));
    }

    @Override
    protected void init() {
        super.init();
        int desiredWidth = Math.max(SCREEN_MIN_WIDTH, this.width - 80);
        int desiredHeight = Math.max(SCREEN_MIN_HEIGHT, this.height - 60);
        this.guiWidth = Math.min(this.width - 20, desiredWidth);
        this.guiHeight = Math.min(this.height - 20, desiredHeight);
        this.guiX = (this.width - this.guiWidth) / 2;
        this.guiY = (this.height - this.guiHeight) / 2;

        this.tabsX = this.guiX + 18;
        this.tabsY = this.guiY + 42;
        this.tabsWidth = this.guiWidth - 36;
        this.tabsHeight = 22;

        this.contentX = this.guiX + 18;
        this.contentY = this.tabsY + this.tabsHeight + 12;
        this.contentWidth = this.guiWidth - 36;
        this.contentHeight = this.guiHeight - 72;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xAA090909);

        context.fill(this.guiX - 2, this.guiY - 2, this.guiX + this.guiWidth + 2, this.guiY + this.guiHeight + 2, ACCENT_SOFT);
        context.fill(this.guiX, this.guiY, this.guiX + this.guiWidth, this.guiY + this.guiHeight, BG);
        context.fill(this.guiX + 14, this.guiY + 14, this.guiX + this.guiWidth - 14, this.guiY + this.guiHeight - 14, PANEL);

        context.drawText(this.textRenderer, Text.literal("Isles+ Dashboard"), this.guiX + 24, this.guiY + 20, TEXT_PRIMARY, true);
        context.drawText(this.textRenderer, Text.literal("Clean controls for your island tools"), this.guiX + 24, this.guiY + 30, TEXT_MUTED, false);
        drawTabs(context, mouseX, mouseY);
        drawFeatureCards(context, mouseX, mouseY);
        drawAuthorSignature(context);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawAuthorSignature(DrawContext context) {
        String authorLabel = "by chrrisk & Scrolls";
        int labelWidth = this.textRenderer.getWidth(authorLabel);
        int baseX = this.guiX + this.guiWidth - 20 - Math.round(labelWidth * AUTHOR_TEXT_SCALE);
        int baseY = this.guiY + this.guiHeight - 20 - Math.round(this.textRenderer.fontHeight * AUTHOR_TEXT_SCALE);
        float hue = (Util.getMeasuringTimeMs() % 4000L) / 4000.0F;
        int authorTextColor = 0xFF000000 | MathHelper.hsvToRgb(hue, 1.0F, 1.0F);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(AUTHOR_TEXT_SCALE, AUTHOR_TEXT_SCALE);
        context.drawText(
            this.textRenderer,
            Text.literal(authorLabel),
            Math.round(baseX / AUTHOR_TEXT_SCALE),
            Math.round(baseY / AUTHOR_TEXT_SCALE),
            authorTextColor,
            false
        );
        context.getMatrices().popMatrix();
    }

    private void drawTabs(DrawContext context, int mouseX, int mouseY) {
        int tabCount = 3;
        int gap = 8;
        int tabWidth = (this.tabsWidth - gap * (tabCount - 1)) / tabCount;
        drawTab(context, this.tabsX, this.tabsY, tabWidth, this.tabsHeight, Tab.QOL, mouseX, mouseY, "QOL");
        drawTab(context, this.tabsX + tabWidth + gap, this.tabsY, tabWidth, this.tabsHeight, Tab.NODE_FARMING, mouseX, mouseY, "Node Farming");
        drawTab(context, this.tabsX + (tabWidth + gap) * 2, this.tabsY, tabWidth, this.tabsHeight, Tab.RIFT, mouseX, mouseY, "Rift");
    }

    private void drawTab(DrawContext context, int x, int y, int width, int height, Tab tab, int mouseX, int mouseY, String label) {
        boolean active = this.activeTab == tab;
        boolean hovered = isInside(mouseX, mouseY, x, y, width, height);
        int fill = active ? ACCENT : hovered ? PANEL_HOVER : PANEL_SOFT;
        int text = active ? 0xFFFFFFFF : TEXT_MUTED;

        context.fill(x, y, x + width, y + height, fill);
        context.fill(x, y + height - 1, x + width, y + height, active ? 0xFFFFFFFF : BORDER);
        int tx = x + (width - this.textRenderer.getWidth(label)) / 2;
        context.drawText(this.textRenderer, Text.literal(label), tx, y + 7, text, false);
    }

    private void drawFeatureCards(DrawContext context, int mouseX, int mouseY) {
        List<FeatureCard> activeCards = cards.getActiveCards(activeTab);
        int cardsPerRow = 2;
        int gap = 10;
        int cardWidth = (this.contentWidth - gap) / cardsPerRow;
        int cardHeight = 44;

        int[] rowHeights = computeRowHeights(activeCards, cardsPerRow);

        for (int i = 0; i < activeCards.size(); i++) {
            FeatureCard card = activeCards.get(i);
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int x = this.contentX + col * (cardWidth + gap);
            int y = this.contentY + rowY(rowHeights, row, gap);
            if (card.controlType == FeatureCard.ControlType.EXPANDABLE) {
                drawExpandableCard(context, card, x, y, cardWidth, mouseX, mouseY);
            } else {
                boolean hovered = isInside(mouseX, mouseY, x, y, cardWidth, cardHeight);
                drawFeatureCard(context, card, x, y, cardWidth, cardHeight, hovered);
            }
        }
    }

    private void drawExpandableCard(DrawContext context, FeatureCard card, int x, int y, int width, int mouseX, int mouseY) {
        boolean killed = card.isKilled();
        boolean expanded = !killed && expandedCards.contains(card);
        int h = expanded ? getExpandedCardHeight(card) : 44;
        boolean hovered = isInside(mouseX, mouseY, x, y, width, h);
        int cardBg = killed ? 0xEE1A1A1A : hovered ? PANEL_HOVER : PANEL_SOFT;
        context.fill(x, y, x + width, y + h, cardBg);
        context.fill(x, y, x + width, y + 1, BORDER);
        context.fill(x, y + h - 1, x + width, y + h, BORDER);

        boolean enabled = !killed && card.enabledSupplier != null && card.enabledSupplier.getAsBoolean();

        // Icon - same as regular cards, with enchant glint when enabled
        ItemStack icon = new ItemStack(card.iconSupplier.get());
        if (enabled) {
            icon.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        context.drawItem(icon, x + 10, y + 13);

        int textColor = killed ? TEXT_MUTED : TEXT_PRIMARY;
        context.drawText(this.textRenderer, Text.literal(card.label), x + 34, y + 10, textColor, false);
        context.drawText(this.textRenderer, Text.literal(card.description), x + 34, y + 22, killed ? 0xFF666666 : TEXT_MUTED, false);
        if (!killed && card.note != null) {
            context.drawText(this.textRenderer, Text.literal(card.note), x + 34, y + 32, TEXT_WARNING, false);
        }

        if (killed) {
            String disabledText = "Disabled";
            int dw = this.textRenderer.getWidth(disabledText);
            int dx = x + width - dw - 10;
            int dy = y + (44 - this.textRenderer.fontHeight) / 2;
            context.drawText(this.textRenderer, Text.literal(disabledText), dx, dy, 0xFFE74C3C, false);
        } else {
            int toggleW = 42, toggleH = 16;
            int tx = x + width - toggleW - 10;
            int ty = y + (44 - toggleH) / 2;

            // Chevron - pixel-drawn triangle, in the left margin before the icon
            drawChevron(context, x + 5, y + 22, expanded, TEXT_MUTED);

            drawToggle(context, tx, ty, toggleW, toggleH, enabled);
        }

        if (!expanded) return;

        context.fill(x + 8, y + 44, x + width - 8, y + 45, BORDER);

        int bodyY = y + 44 + 6;

        if (card.expandOptions != null && card.optionSuppliers != null) {
            for (int i = 0; i < card.expandOptions.length; i++) {
                int optY = bodyY + i * 18;
                context.drawText(this.textRenderer, Text.literal(card.expandOptions[i]), x + 14, optY + 5, TEXT_PRIMARY, false);
                int cbSize = 12;
                int cbX = x + width - cbSize - 14;
                int cbY = optY + (18 - cbSize) / 2;
                drawCheckbox(context, cbX, cbY, cbSize, card.optionSuppliers[i].getAsBoolean());
            }
            bodyY += card.expandOptions.length * 18;
        }

        bodyY += 6;

        if (card.sliderLabel != null && card.sliderSupplier != null) {
            int midY = bodyY + 12;
            int labelW = this.textRenderer.getWidth(card.sliderLabel);
            context.drawText(this.textRenderer, Text.literal(card.sliderLabel), x + 14, midY - 4, TEXT_MUTED, false);

            int trackX = x + 14 + labelW + 8;
            int trackEnd = x + width - 14;
            int trackW = trackEnd - trackX;
            int knobX = trackX + (int)(card.sliderSupplier.get() * trackW);

            if (card.huePicker) {
                // Rainbow hue strip
                for (int px = 0; px < trackW; px += 2) {
                    int color = 0xFF000000 | MathHelper.hsvToRgb((float) px / trackW, 1.0f, 1.0f);
                    context.fill(trackX + px, midY - 5, Math.min(trackX + px + 2, trackEnd), midY + 5, color);
                }
                // Knob: black border + white inner
                context.fill(knobX - 3, midY - 7, knobX + 3, midY + 7, 0xFF000000);
                context.fill(knobX - 2, midY - 6, knobX + 2, midY + 6, 0xFFFFFFFF);
            } else {
                context.fill(trackX, midY - 1, trackEnd, midY + 1, BORDER);
                context.fill(knobX - 4, midY - 4, knobX + 4, midY + 4, TEXT_PRIMARY);
            }
            bodyY += 24;
        }

        if (card.legendLines != null) {
            for (String line : card.legendLines) {
                context.drawText(this.textRenderer, Text.literal(line), x + 14, bodyY + 2, TEXT_MUTED, false);
                bodyY += 12;
            }
        }
    }

    private void drawFeatureCard(DrawContext context, FeatureCard card, int x, int y, int width, int height, boolean hovered) {
        boolean killed = card.isKilled();
        boolean enabled = !killed && card.enabledSupplier != null && card.enabledSupplier.getAsBoolean();
        int cardBg = killed ? 0xEE1A1A1A : hovered ? PANEL_HOVER : PANEL_SOFT;
        context.fill(x, y, x + width, y + height, cardBg);
        context.fill(x, y, x + width, y + 1, BORDER);
        context.fill(x, y + height - 1, x + width, y + height, BORDER);

        ItemStack icon = new ItemStack(card.iconSupplier.get());
        if (enabled) {
            icon.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        context.drawItem(icon, x + 10, y + 13);

        int textColor = killed ? TEXT_MUTED : TEXT_PRIMARY;
        context.drawText(this.textRenderer, Text.literal(card.label), x + 34, y + 10, textColor, false);
        int descColor = killed ? 0xFF666666 : card.redDescription ? 0xFFFF4D4D : TEXT_MUTED;
        context.drawText(this.textRenderer, Text.literal(card.description), x + 34, y + 22, descColor, false);
        if (!killed && card.note != null) {
            context.drawText(this.textRenderer, Text.literal(card.note), x + 34, y + 32, TEXT_WARNING, false);
        }

        if (killed) {
            String disabledText = "Disabled";
            int dw = this.textRenderer.getWidth(disabledText);
            int dx = x + width - dw - 10;
            int dy = y + (height - this.textRenderer.fontHeight) / 2;
            context.drawText(this.textRenderer, Text.literal(disabledText), dx, dy, 0xFFE74C3C, false);
        } else {
            int toggleW = 42;
            int toggleH = 16;
            int tx = x + width - toggleW - 10;
            int ty = y + (height - toggleH) / 2;
            drawToggle(context, tx, ty, toggleW, toggleH, enabled);
        }
    }

    private void drawToggle(DrawContext context, int x, int y, int w, int h, boolean enabled) {
        int track = enabled ? POSITIVE : 0xFF5B6678;
        context.fill(x, y, x + w, y + h, track);
        int knobSize = h - 4;
        int knobX = enabled ? x + w - knobSize - 2 : x + 2;
        context.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, 0xFFFFFFFF);
    }

    /** Pixel-drawn triangle chevron. {@code down=true} → ▼ (expanded), {@code down=false} → ▶ (collapsed). */
    private void drawChevron(DrawContext context, int cx, int cy, boolean down, int color) {
        if (down) {
            // Down-pointing triangle, 7px wide, 4px tall
            context.fill(cx - 3, cy - 1, cx + 4, cy,     color); // 7 wide
            context.fill(cx - 2, cy,     cx + 3, cy + 1, color); // 5 wide
            context.fill(cx - 1, cy + 1, cx + 2, cy + 2, color); // 3 wide
            context.fill(cx,     cy + 2, cx + 1, cy + 3, color); // 1 wide (tip)
        } else {
            // Right-pointing triangle, 4px wide, 7px tall
            context.fill(cx - 1, cy - 3, cx,     cy + 4, color); // 7 tall
            context.fill(cx,     cy - 2, cx + 1, cy + 3, color); // 5 tall
            context.fill(cx + 1, cy - 1, cx + 2, cy + 2, color); // 3 tall
            context.fill(cx + 2, cy,     cx + 3, cy + 1, color); // 1 tall (tip)
        }
    }

    private void drawCheckbox(DrawContext context, int x, int y, int size, boolean checked) {
        context.fill(x, y, x + size, y + size, BORDER);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, PANEL_SOFT);
        if (checked) {
            context.fill(x + 2, y + 2, x + size - 2, y + size - 2, POSITIVE);
        }
    }

    private int[] computeRowHeights(List<FeatureCard> activeCards, int cardsPerRow) {
        int rows = (activeCards.size() + cardsPerRow - 1) / cardsPerRow;
        int[] heights = new int[rows];
        for (int i = 0; i < activeCards.size(); i++) {
            int row = i / cardsPerRow;
            FeatureCard card = activeCards.get(i);
            int h = (card.controlType == FeatureCard.ControlType.EXPANDABLE && expandedCards.contains(card))
                ? getExpandedCardHeight(card) : 44;
            heights[row] = Math.max(heights[row], h);
        }
        return heights;
    }

    private int rowY(int[] rowHeights, int row, int gap) {
        int y = 0;
        for (int r = 0; r < row; r++) y += rowHeights[r] + gap;
        return y;
    }

    private int getExpandedCardHeight(FeatureCard card) {
        int h = 44 + 6; // header + top body padding
        if (card.expandOptions != null) h += card.expandOptions.length * 18;
        h += 6; // padding before slider
        if (card.sliderLabel != null) h += 24;
        if (card.legendLines != null) h += card.legendLines.length * 12;
        h += 8; // bottom padding
        return h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        int tabCount = 3;
        int gap = 8;
        int tabWidth = (this.tabsWidth - gap * (tabCount - 1)) / tabCount;
        Tab[] tabs = new Tab[]{Tab.QOL, Tab.NODE_FARMING, Tab.RIFT};
        for (int i = 0; i < tabs.length; i++) {
            int tabX = this.tabsX + i * (tabWidth + gap);
            if (isInside(mouseX, mouseY, tabX, this.tabsY, tabWidth, this.tabsHeight)) {
                this.activeTab = tabs[i];
                IslesClient.playMenuClickSound();
                return true;
            }
        }

        if (onFeatureCardClicked(mouseX, mouseY)) {
            IslesClient.playMenuClickSound();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean onFeatureCardClicked(int mouseX, int mouseY) {
        int cardsPerRow = 2;
        int gap = 10;
        int cardWidth = (this.contentWidth - gap) / cardsPerRow;
        int cardHeight = 44;

        List<FeatureCard> activeCards = cards.getActiveCards(activeTab);
        int[] rowHeights = computeRowHeights(activeCards, cardsPerRow);

        for (int i = 0; i < activeCards.size(); i++) {
            FeatureCard card = activeCards.get(i);
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int x = this.contentX + col * (cardWidth + gap);
            int y = this.contentY + rowY(rowHeights, row, gap);

            if (card.controlType == FeatureCard.ControlType.EXPANDABLE) {
                boolean expanded = expandedCards.contains(card);
                int h = expanded ? getExpandedCardHeight(card) : cardHeight;
                if (!isInside(mouseX, mouseY, x, y, cardWidth, h)) continue;

                if (card.isKilled()) return true; // consume click but do nothing

                // Toggle button (always in header)
                int toggleW = 42, toggleH = 16;
                int tx = x + cardWidth - toggleW - 10;
                int ty = y + (cardHeight - toggleH) / 2;
                if (isInside(mouseX, mouseY, tx, ty, toggleW, toggleH)) {
                    card.toggle();
                    return true;
                }

                // Body interactions (only when expanded and click is below header)
                if (expanded && mouseY >= y + cardHeight) {
                    int bodyY = y + cardHeight + 6;

                    if (card.expandOptions != null && card.optionTogglers != null) {
                        for (int j = 0; j < card.expandOptions.length; j++) {
                            int optY = bodyY + j * 18;
                            int cbSize = 12;
                            int cbX = x + cardWidth - cbSize - 14;
                            int cbY = optY + (18 - cbSize) / 2;
                            if (isInside(mouseX, mouseY, cbX, cbY, cbSize, cbSize)) {
                                card.optionTogglers[j].run();
                                return true;
                            }
                        }
                        bodyY += card.expandOptions.length * 18;
                    }

                    bodyY += 6;
                    if (card.sliderLabel != null && card.sliderConsumer != null) {
                        int midY = bodyY + 12;
                        int trackX = x + 14 + this.textRenderer.getWidth(card.sliderLabel) + 8;
                        int trackW = (x + cardWidth - 14) - trackX;
                        if (isInside(mouseX, mouseY, trackX, midY - 8, trackW, 16)) {
                            float val = Math.max(0f, Math.min(1f, (float)(mouseX - trackX) / trackW));
                            card.sliderConsumer.accept(val);
                            this.dragSliderTrackX = trackX;
                            this.dragSliderTrackWidth = trackW;
                            this.activeSliderConsumer = card.sliderConsumer;
                            sliderDragging = true;
                            return true;
                        }
                    }

                    return true; // consumed - body click with no specific element
                }

                // Header click (outside toggle) -> expand/collapse
                if (expanded) expandedCards.remove(card); else expandedCards.add(card);
                return true;
            }

            if (isInside(mouseX, mouseY, x, y, cardWidth, cardHeight)) {
                if (!card.isKilled()) card.toggle();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (sliderDragging && activeSliderConsumer != null && dragSliderTrackWidth > 0) {
            float val = Math.max(0f, Math.min(1f, (float)(click.x() - dragSliderTrackX) / dragSliderTrackWidth));
            activeSliderConsumer.accept(val);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (activeSliderConsumer != null) {
            IslesPlusConfig.save();
        }
        sliderDragging = false;
        activeSliderConsumer = null;
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
