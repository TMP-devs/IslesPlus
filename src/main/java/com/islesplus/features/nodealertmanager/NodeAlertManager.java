package com.islesplus.features.nodealertmanager;

import com.islesplus.IslesClient;
import com.islesplus.sound.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

public class NodeAlertManager {
    static final long ALERT_REPEAT_MS = 667L;
    static final long REGEN_REMINDER_REPEAT_MS = 1_200L;
    static final long SKILL_ALERT_MIN_DURATION_MS = 3_000L;

    // public so IslesScreen (com.islesplus.screen) can read/write
    public static boolean depletionPingEnabled = false;
    public static float depletionPingVolume = 0.5f;
    public static RegenPingMode regenPingMode = RegenPingMode.OFF;
    public static float regenPingVolume = 0.5f;
    public static boolean regenReminderActive = false;

    private static ActiveSkillAlert activeSkillAlert = ActiveSkillAlert.NONE;
    private static long activeSkillAlertStartedMs = 0L;
    private static long activeSkillAlertLastSoundMs = 0L;
    public static long lastRegenReminderMs = 0L;

    public enum ActiveSkillAlert {
        NONE,
        NODE_DEPLETED,
        NODE_REGENERATED
    }

    public enum RegenPingMode {
        SHORT_PING,
        PING_UNTIL_INTERACT,
        OFF
    }

    public static void tickSkillAlert(MinecraftClient client) {
        if (activeSkillAlert == ActiveSkillAlert.NONE || client.player == null) {
            return;
        }

        long now = Util.getMeasuringTimeMs();
        if (now - activeSkillAlertLastSoundMs >= ALERT_REPEAT_MS) {
            ModSounds.playScaled(client, ModSounds.Cue.NODE_ALERT, regenPingVolume);
            activeSkillAlertLastSoundMs = now;
        }

        if (now - activeSkillAlertStartedMs < SKILL_ALERT_MIN_DURATION_MS) {
            return;
        }
        stopSkillAlert();
    }

    public static void tickRegenReminder(MinecraftClient client) {
        if (!regenReminderActive || client.player == null || NodeTracker.trackedNode == null) {
            return;
        }
        if (!NodeTracker.nodeUpdatesEnabled || regenPingMode != RegenPingMode.PING_UNTIL_INTERACT) {
            regenReminderActive = false;
            return;
        }
        if (NodeTracker.selfActivelyFarmingTrackedNode) {
            regenReminderActive = false;
            IslesClient.sendStatusMessage(client, "Regen reminder cleared: you started farming " + NodeTracker.trackedNode.nodeName);
            return;
        }

        long now = Util.getMeasuringTimeMs();
        if (now - lastRegenReminderMs >= REGEN_REMINDER_REPEAT_MS) {
            ModSounds.playScaled(client, ModSounds.Cue.NODE_ALERT, regenPingVolume);
            lastRegenReminderMs = now;
        }
    }

    public static void startSkillAlert(MinecraftClient client, ActiveSkillAlert alertType) {
        if (client.player == null || alertType == ActiveSkillAlert.NONE) {
            return;
        }
        if (alertType == ActiveSkillAlert.NODE_DEPLETED) {
            ModSounds.playScaled(client, ModSounds.Cue.NODE_DEPLETED, depletionPingVolume);
            IslesClient.sendStatusMessage(client, "Node depleted detected.");
            return;
        }
        if (activeSkillAlert != ActiveSkillAlert.NONE) {
            return;
        }

        activeSkillAlert = alertType;
        activeSkillAlertStartedMs = Util.getMeasuringTimeMs();
        activeSkillAlertLastSoundMs = 0L;

        String message = alertType == ActiveSkillAlert.NODE_DEPLETED
            ? "Node depleted detected."
            : "Node regeneration detected.";
        IslesClient.sendStatusMessage(client, message);
    }

    public static void stopSkillAlert() {
        activeSkillAlert = ActiveSkillAlert.NONE;
        activeSkillAlertStartedMs = 0L;
        activeSkillAlertLastSoundMs = 0L;
    }

    public static RegenPingMode nextRegenPingMode(RegenPingMode mode) {
        return switch (mode) {
            case SHORT_PING -> RegenPingMode.PING_UNTIL_INTERACT;
            case PING_UNTIL_INTERACT -> RegenPingMode.OFF;
            case OFF -> RegenPingMode.SHORT_PING;
        };
    }

    public static String regenPingModeLabel() {
        return switch (regenPingMode) {
            case SHORT_PING -> "Short Ping";
            case PING_UNTIL_INTERACT -> "Ping-Until-Interact";
            case OFF -> "Off";
        };
    }

    public static int regenPingModeColor() {
        return switch (regenPingMode) {
            case SHORT_PING -> 0xFF2ECC71;
            case PING_UNTIL_INTERACT -> 0xFFF1C40F;
            case OFF -> 0xFFE74C3C;
        };
    }

    public static void reset() {
        activeSkillAlert = ActiveSkillAlert.NONE;
        activeSkillAlertStartedMs = 0L;
        activeSkillAlertLastSoundMs = 0L;
        regenReminderActive = false;
        lastRegenReminderMs = 0L;
    }
}
