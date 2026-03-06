package com.islesplus.features.harvesttimer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

public final class NerdModeActivator {
    private static final Identifier GRAY_DYE = Identifier.of("minecraft", "gray_dye");
    private static final String NERD_MODE_NAME = "Nerd Mode";
    private static final int SCAN_DELAY_TICKS = 5;

    private static State state = State.IDLE;
    private static int tickCounter = 0;

    private enum State { IDLE, WAITING_FOR_SCREEN, SCANNING }

    private NerdModeActivator() {}

    public static void activate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        state = State.WAITING_FOR_SCREEN;
        tickCounter = 0;
        client.player.networkHandler.sendChatCommand("settings");
    }

    public static void onScreenOpen(HandledScreen<?> screen) {
        if (state != State.WAITING_FOR_SCREEN) return;
        state = State.SCANNING;
        tickCounter = 0;
    }

    public static void onScreenTick(HandledScreen<?> screen) {
        if (state != State.SCANNING) return;
        tickCounter++;
        if (tickCounter < SCAN_DELAY_TICKS) return;

        state = State.IDLE;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            if (!stack.getName().getString().equals(NERD_MODE_NAME)) continue;

            Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
            if (GRAY_DYE.equals(model)) {
                // Nerd mode is off - click to enable
                client.interactionManager.clickSlot(
                    screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.PICKUP, client.player
                );
            }
            // Either way, close the screen
            client.player.closeHandledScreen();
            return;
        }

        // Nerd mode slot not found - close anyway
        client.player.closeHandledScreen();
    }

    public static void reset() {
        state = State.IDLE;
        tickCounter = 0;
    }
}
