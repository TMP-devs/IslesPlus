package com.islesplus.screen.islesscreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

class RiftWarningScreen extends Screen {
    private static final int DIALOG_W = 270;
    private static final int DIALOG_H = 148;

    private static final Text TITLE = Text.literal("[ ! ]  Heads Up")
        .formatted(Formatting.RED, Formatting.BOLD);
    private static final String BODY =
        "Rift features provide gameplay assistance that may affect your " +
        "experience. The Rift is designed to be discovered naturally. " +
        "We recommend experiencing it unaided before enabling these.";

    private final Screen parent;
    private final Runnable onConfirm;
    private boolean dontShowAgain = false;

    RiftWarningScreen(Screen parent, Runnable onConfirm) {
        super(Text.empty());
        this.parent = parent;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();
        int dx = (width  - DIALOG_W) / 2;
        int dy = (height - DIALOG_H) / 2;

        addDrawableChild(ButtonWidget.builder(dontShowLabel(), btn -> {
            dontShowAgain = !dontShowAgain;
            btn.setMessage(dontShowLabel());
        }).dimensions(dx + (DIALOG_W - 170) / 2, dy + DIALOG_H - 46, 170, 16).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(dx + 8, dy + DIALOG_H - 26, 80, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Enable"), btn -> {
            if (dontShowAgain) RiftWarningManager.setDismissed();
            onConfirm.run();
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(dx + DIALOG_W - 88, dy + DIALOG_H - 26, 80, 18).build());
    }

    private Text dontShowLabel() {
        return Text.literal((dontShowAgain ? "[x]" : "[ ]") + " Don't show this again")
            .formatted(Formatting.GRAY);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);

        int dx = (width  - DIALOG_W) / 2;
        int dy = (height - DIALOG_H) / 2;

        // Background
        ctx.fill(dx, dy, dx + DIALOG_W, dy + DIALOG_H, 0xFF111120);

        // Border
        ctx.fill(dx,                dy,                dx + DIALOG_W,     dy + 1,            0xFFCC3333);
        ctx.fill(dx,                dy + DIALOG_H - 1, dx + DIALOG_W,     dy + DIALOG_H,     0xFFCC3333);
        ctx.fill(dx,                dy,                dx + 1,            dy + DIALOG_H,     0xFFCC3333);
        ctx.fill(dx + DIALOG_W - 1, dy,                dx + DIALOG_W,     dy + DIALOG_H,     0xFFCC3333);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, TITLE, width / 2, dy + 10, 0xFFFFFF);

        // Divider
        ctx.fill(dx + 8, dy + 22, dx + DIALOG_W - 8, dy + 23, 0x44FFFFFF);

        // Body text
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(BODY), DIALOG_W - 20);
        int ty = dy + 29;
        for (OrderedText line : lines) {
            ctx.drawText(textRenderer, line, dx + 10, ty, 0xFFBBBBBB, true);
            ty += textRenderer.fontHeight + 2;
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
