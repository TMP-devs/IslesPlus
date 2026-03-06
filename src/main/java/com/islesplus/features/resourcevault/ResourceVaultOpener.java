package com.islesplus.features.resourcevault;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public final class ResourceVaultOpener {
    private static final String BUTTON_NAME = "Resource Storage";
    private static final int SCAN_DELAY_TICKS = 5;

    private static State state = State.IDLE;
    private static int tickCounter = 0;
    private static int expectedSyncId = -1;

    private enum State { IDLE, WAITING_FOR_SCREEN, SCANNING }

    private ResourceVaultOpener() {}

    public static void activate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        state = State.WAITING_FOR_SCREEN;
        tickCounter = 0;
        client.execute(() -> client.setScreen(new InventoryScreen(client.player)));
    }

    public static void onScreenOpen(HandledScreen<?> screen) {
        if (state != State.WAITING_FOR_SCREEN) return;
        if (!(screen instanceof InventoryScreen)) { state = State.IDLE; return; }
        expectedSyncId = screen.getScreenHandler() != null ? screen.getScreenHandler().syncId : -1;
        state = State.SCANNING;
        tickCounter = 0;
    }

    public static void onScreenTick(HandledScreen<?> screen) {
        if (state != State.SCANNING) return;
        if (screen.getScreenHandler() == null) { state = State.IDLE; return; }
        if (screen.getScreenHandler().syncId != expectedSyncId) { state = State.IDLE; return; }
        tickCounter++;
        if (tickCounter < SCAN_DELAY_TICKS) return;

        state = State.IDLE;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            if (!stack.getName().getString().equals(BUTTON_NAME)) continue;
            client.interactionManager.clickSlot(
                screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.PICKUP, client.player
            );
            return;
        }
    }

    public static void reset() {
        state = State.IDLE;
        tickCounter = 0;
        expectedSyncId = -1;
    }
}
