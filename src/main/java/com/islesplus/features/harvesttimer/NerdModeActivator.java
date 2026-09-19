package com.islesplus.features.harvesttimer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

            if (isToggleOff(stack)) {
                // nerd mode is off, click it on
                client.interactionManager.clickSlot(
                    screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.PICKUP, client.player
                );
            }
            // either way we're done, close it
            client.player.closeHandledScreen();
            return;
        }

        // couldn't find the nerd mode slot, close anyway
        client.player.closeHandledScreen();
    }

    // /settings toggles used to be gray dye (off) / lime dye (on) via item_model, the newer menu
    // uses a plain ghast tear for off and paper for on (same as the plushie menu). handle both
    private static boolean isToggleOff(ItemStack stack) {
        if (stack.isOf(Items.GHAST_TEAR)) return true;
        Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
        return GRAY_DYE.equals(model);
    }

    public static void reset() {
        state = State.IDLE;
        tickCounter = 0;
    }
}
