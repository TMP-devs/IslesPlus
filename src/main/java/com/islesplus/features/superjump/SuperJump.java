package com.islesplus.features.superjump;

import com.islesplus.IslesClient;
import com.islesplus.sync.FeatureFlags;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * One key for the server's super jump, which is normally two keys hit together: jump and
 * swap-to-off-hand. It works the way the "Multi Key Bindings" mod does, as if the Isles+ key were
 * bound to BOTH of those actions: the instant the physical key goes down (the raw key event, not
 * the next game tick) both bindings count as pressed, swap-hands gets its one click, and both stay
 * held for exactly as long as the key is held. Letting go releases both.
 * <p>Nothing is timed or sequenced: pressing jump even one tick before the off-hand key ruins it
 * (the dash then fires from mid-air), and a fixed hold time is not what a key does.
 * <p>Keyboard auto-repeat is ignored. Unbound by default, so it does nothing until the player gives
 * it a key; it also does nothing unless both vanilla keys are bound.
 */
public final class SuperJump {
    /** Whether our key is physically down and currently holding the two vanilla bindings. */
    private static boolean holding = false;
    /** A game tick has run with the bindings held. A tap can start AND end between two ticks; the
     * release then waits for that tick, or the jump would never be seen at all. */
    private static boolean tickSeen = false;
    private static boolean releasePending = false;

    private SuperJump() {}

    /** From {@code KeyBindingMixin}: every raw press / release of any key or mouse button. */
    public static void onRawKey(InputUtil.Key key, boolean pressed) {
        if (IslesClient.SUPER_JUMP_KEY.isUnbound()) return;
        if (!key.equals(KeyBindingHelper.getBoundKeyOf(IslesClient.SUPER_JUMP_KEY))) return;
        MinecraftClient client = MinecraftClient.getInstance();

        if (!pressed) {
            if (holding && !tickSeen) releasePending = true;
            else release(client);
            return;
        }
        if (holding) return;   // keyboard auto-repeat: the key never came up in between
        if (FeatureFlags.isKilled("super_jump")) return;
        if (client.player == null || client.currentScreen != null) return;
        if (unboundWarning() != null) return;   // needs BOTH keys: never press just one of them

        holding = true;
        tickSeen = false;
        releasePending = false;
        // Off hand: held, plus the one click vanilla acts on (it swaps per click, not per held tick).
        client.options.swapHandsKey.setPressed(true);
        KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(client.options.swapHandsKey));
        // Jump: held, which is all vanilla reads for jumping.
        client.options.jumpKey.setPressed(true);
    }

    /** Lets go of both and hands them back to whatever the player is really holding. */
    private static void release(MinecraftClient client) {
        if (!holding) return;
        holding = false;
        releasePending = false;
        if (client == null || client.options == null) return;
        client.options.jumpKey.setPressed(false);
        client.options.swapHandsKey.setPressed(false);
        // Under a screen nothing is re-read: that would press held keys "through" the screen.
        if (client.currentScreen == null) KeyBinding.updatePressedStates();
    }

    /** Safety net, once a tick: a release event can be lost (the window lost focus, a screen
     * opened), and without this the key would look held forever and never fire again. */
    public static void tick(MinecraftClient client) {
        if (!holding) return;
        tickSeen = true;
        if (releasePending || !IslesClient.SUPER_JUMP_KEY.isPressed()) release(client);
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

    public static void reset() {
        release(MinecraftClient.getInstance());
        holding = false;
    }
}
