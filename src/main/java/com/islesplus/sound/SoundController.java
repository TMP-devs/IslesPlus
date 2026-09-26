package com.islesplus.sound;

import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;

/**
 * "Mod-only sounds": in an Isles world, the only sounds you hear are the ones Isles+ plays (node
 * pings, alerts, its menu clicks) plus the game's own menu clicks (UI category), so menus still
 * click. Everything else - world, mobs, blocks, music, and every sound the server sends - is muted.
 * The hub (world OTHER) is never touched.
 */
public final class SoundController {
    private static boolean modOnlySoundsEnabled = false;

    private SoundController() {
    }

    public static void setModOnlySoundsEnabled(boolean enabled) {
        modOnlySoundsEnabled = enabled;
    }

    public static boolean isModOnlySoundsEnabled() {
        return modOnlySoundsEnabled;
    }

    private static boolean filtering() {
        return modOnlySoundsEnabled && WorldIdentification.world != PlayerWorld.OTHER;
    }

    /** A sound packet from the server: never ours, so muted whenever the filter is on. */
    public static boolean shouldMuteIncomingSound() {
        return filtering();
    }

    /** A sound about to start on this client. */
    public static boolean shouldAllowLocalSound(boolean uiCategory) {
        return allow(filtering(), ModSounds.isPlayingOwn(), uiCategory);
    }

    static boolean allow(boolean filtering, boolean own, boolean uiCategory) {
        return !filtering || own || uiCategory;
    }
}
