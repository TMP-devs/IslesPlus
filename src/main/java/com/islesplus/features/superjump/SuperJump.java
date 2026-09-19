package com.islesplus.features.superjump;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * One key for the server's super jump, which is normally two keys at once: swap-to-off-hand and
 * jump. Pressing the Isles+ key presses both of those key bindings together, exactly as if the
 * player had hit them at the same moment - one press each, once per physical press of our key (holding it does not repeat). It is
 * unbound by default, so it does nothing until the player gives it a key.
 */
public final class SuperJump {
    private static final int JUMP_HOLD_TICKS = 2;
    private static int jumpTicksLeft = 0;

    private SuperJump() {}

    private static boolean keyWasDown = false;

    /** Call every tick with whether the super jump key is physically down, and whether Minecraft
     * counted a press since the last tick. Fires once per physical press: holding the key (the OS
     * repeats it, and each repeat counts as a "press" to Minecraft) must not swap and jump over
     * and over, while a tap that is already released by the time the tick samples it still counts. */
    public static void onKeyState(MinecraftClient client, boolean down, boolean tapped) {
        boolean pressedNow = (down || tapped) && !keyWasDown;
        keyWasDown = down;
        if (pressedNow) trigger(client);
    }

    private static void trigger(MinecraftClient client) {
        if (FeatureFlags.isKilled("super_jump")) return;
        if (client.player == null || client.currentScreen != null) return;
        if (unboundWarning() != null) return;   // needs BOTH keys: never press just one of them

        // Off hand: register one press of whatever key "Swap Item With Offhand" is bound to.
        // Vanilla then handles it like a real press, this same tick.
        InputUtil.Key swapKey = KeyBindingHelper.getBoundKeyOf(client.options.swapHandsKey);
        KeyBinding.onKeyPressed(swapKey);

        // Jump: hold the jump binding down for a couple of ticks, then let go (see tick()).
        client.options.jumpKey.setPressed(true);
        jumpTicksLeft = JUMP_HOLD_TICKS;
    }

    /** Which of the two vanilla keys is unbound, as a sentence for the Keybinds card; null when
     * both are bound and the super jump can run. */
    public static String unboundWarning() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return null;
        boolean noJump = client.options.jumpKey.isUnbound();
        boolean noSwap = client.options.swapHandsKey.isUnbound();
        if (!noJump && !noSwap) return null;
        String which = noJump && noSwap ? "Jump and Swap Item With Offhand are" : noJump ? "Jump is" : "Swap Item With Offhand is";
        return which + " unbound in Controls, so Super Jump will not run.";
    }

    /** Lets go of the jump key again, handing it back to whatever the player is really holding. */
    public static void tick(MinecraftClient client) {
        if (jumpTicksLeft <= 0) return;
        if (--jumpTicksLeft == 0) releaseJump(client);
    }

    /** Lets go of jump. With no screen open the key is handed back to whatever the player is
     * really holding; under a screen it is simply released (re-reading the keyboard there would
     * press held keys "through" the screen). */
    private static void releaseJump(MinecraftClient client) {
        client.options.jumpKey.setPressed(false);
        if (client.currentScreen == null) KeyBinding.updatePressedStates();
    }

    public static void reset() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (jumpTicksLeft > 0 && client != null && client.options != null) releaseJump(client);
        jumpTicksLeft = 0;
        keyWasDown = false;
    }
}
