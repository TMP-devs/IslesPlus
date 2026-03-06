package com.islesplus.features.dropnotifier;

import com.islesplus.IslesClient;
import com.islesplus.sound.ModSounds;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;

import java.util.Locale;

public class DropNotifier {
    private static final double ENTITY_SCAN_RADIUS = 7.0;
    private static final long DROP_NOTIFY_COOLDOWN_MS = 2_500L;

    // public so IslesScreen (com.islesplus.screen) can read/write
    public static boolean dropNotifyEnabled = false;
    public static float dropNotifyVolume = 0.5f;
    private static long lastDropNotifyMs = 0L;

    public static void tick(MinecraftClient client) {
        if (!dropNotifyEnabled || WorldIdentification.world != PlayerWorld.ISLE || client.player == null || client.world == null) {
            return;
        }

        long now = Util.getMeasuringTimeMs();
        if (now - lastDropNotifyMs < DROP_NOTIFY_COOLDOWN_MS) {
            return;
        }

        double radiusSq = ENTITY_SCAN_RADIUS * ENTITY_SCAN_RADIUS;
        for (Entity entity : client.world.getEntities()) {
            if (!"entity.minecraft.armor_stand".equals(entity.getType().toString())) {
                continue;
            }
            if (client.player.squaredDistanceTo(entity) > radiusSq) {
                continue;
            }

            String label = entity.getName().getString();
            String normalized = label.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("8x ") && !normalized.startsWith("16x ")) {
                continue;
            }

            ModSounds.playScaled(client, ModSounds.Cue.DROP_NOTIFY, dropNotifyVolume);
            lastDropNotifyMs = now;
            IslesClient.sendStatusMessage(client, "Drop notify: " + label);
            return;
        }
    }

    public static void reset() {
        lastDropNotifyMs = 0L;
    }
}
