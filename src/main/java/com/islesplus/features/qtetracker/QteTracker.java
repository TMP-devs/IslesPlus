package com.islesplus.features.qtetracker;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.entity.TrackedNode;
import com.islesplus.features.nodealertmanager.NodeTracker;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QteTracker {
    private static final double POSITION_EPSILON = 0.01;
    private static final double NODE_RADIUS = 10.0;
    private static final double NODE_RADIUS_SQ = NODE_RADIUS * NODE_RADIUS;

    public enum QteType {
        LUCK(0xFFFF55FF),       // pink
        EXP(0xFF55FF55),        // green
        CHANCE(0xFF55FF55),     // green
        COINS(0xFFFFD700),      // gold
        TICK_SKIP(0xFF55FF55);  // green

        public final int color;
        QteType(int color) { this.color = color; }
    }

    public record TrackedQte(int entityId, QteType type, double textDisplayY) {}

    public static boolean qteTrackerEnabled = true;

    public static boolean qteLuckEnabled = true;
    public static boolean qteExpEnabled = false;
    public static boolean qteChanceEnabled = true;
    public static boolean qteCoinsEnabled = false;
    public static boolean qteTickSkipEnabled = false;

    private static volatile List<TrackedQte> tracked = List.of();
    private static final Map<Integer, Long> qteFirstSeenMs = new HashMap<>();
    private static final Set<Integer> ambientBlacklist = new HashSet<>();
    private static UUID lastTrackedNodeUuid = null;
    private static final long QTE_MAX_AGE_MS = 10_000L;

    private QteTracker() {}

    public static List<TrackedQte> getTracked() {
        return tracked;
    }

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        TrackedNode node = NodeTracker.trackedNode;
        if (!qteTrackerEnabled
                || WorldIdentification.world != PlayerWorld.ISLE
                || client.player == null || client.world == null) {
            tracked = List.of();
            return;
        }

        // While not tracking any node, permanently blacklist all nearby interactions  -
        // they are ambient entities (auction houses, shops, etc.), never real QTEs
        if (node == null) {
            for (Entity interaction : scan.interactions) {
                ambientBlacklist.add(interaction.getId());
            }
            tracked = List.of();
            return;
        }

        // Clear QTE history when switching to a different node
        UUID currentNodeUuid = node.textDisplayUuid;
        if (!currentNodeUuid.equals(lastTrackedNodeUuid)) {
            qteFirstSeenMs.clear();
            lastTrackedNodeUuid = currentNodeUuid;
        }

        long now = System.currentTimeMillis();

        // Pre-populate first-seen times for candidate entities as soon as the node is tracked,
        // so the blacklist clock starts before farming begins
        for (Entity interaction : scan.interactions) {
            if (withinNodeRadius(interaction, node)) {
                qteFirstSeenMs.putIfAbsent(interaction.getId(), now);
            }
        }
        for (Entity armorStand : scan.armorStands) {
            if (withinNodeRadius(armorStand, node)) {
                qteFirstSeenMs.putIfAbsent(armorStand.getId(), now);
            }
        }

        if (!NodeTracker.selfActivelyFarmingTrackedNode) {
            tracked = List.of();
            return;
        }

        List<TrackedQte> next = new ArrayList<>();

        // Bonus QTEs: find text_displays with "CLICK ME" within node radius, then co-located armor_stands
        for (Entity textEntity : scan.textDisplaysNearDouble) {
            if (!withinNodeRadius(textEntity, node)) continue;
            if (!(textEntity instanceof DisplayEntity.TextDisplayEntity textDisplay)) continue;
            String text;
            try {
                text = textDisplay.getText().getString();
            } catch (Throwable ignored) {
                continue;
            }
            if (!text.contains("CLICK ME")) continue;

            QteType type = parseQteType(text);
            if (type == null || !isTypeEnabled(type)) continue;

            // Find co-located armor_stand (same X,Z; armor_stand Y < text Y)
            for (Entity armorStand : scan.armorStands) {
                if (Math.abs(armorStand.getX() - textEntity.getX()) <= POSITION_EPSILON
                        && Math.abs(armorStand.getZ() - textEntity.getZ()) <= POSITION_EPSILON
                        && armorStand.getY() < textEntity.getY()) {
                    int id = armorStand.getId();
                    qteFirstSeenMs.putIfAbsent(id, now);
                    if (now - qteFirstSeenMs.get(id) < QTE_MAX_AGE_MS) {
                        next.add(new TrackedQte(id, type, textEntity.getY()));
                    }
                    break;
                }
            }
        }

        // Tick skip QTEs: standalone interactions within node radius, not co-located with node entities
        // and not near a text_display containing a known node name
        if (qteTickSkipEnabled) {
            for (Entity interaction : scan.interactions) {
                if (!withinNodeRadius(interaction, node)) continue;
                if (isColocated(interaction, scan.areaEffectClouds)) continue;
                if (isColocated(interaction, scan.itemDisplays)) continue;
                if (isColocatedXZ(interaction, scan.textDisplaysFar)) continue;
                int id = interaction.getId();
                if (ambientBlacklist.contains(id)) continue;
                qteFirstSeenMs.putIfAbsent(id, now);
                if (now - qteFirstSeenMs.get(id) < QTE_MAX_AGE_MS) {
                    next.add(new TrackedQte(id, QteType.TICK_SKIP, Double.NaN));
                }
            }
        }

        tracked = List.copyOf(next);
    }

    private static boolean withinNodeRadius(Entity entity, TrackedNode node) {
        double dx = entity.getX() - node.nodeX;
        double dz = entity.getZ() - node.nodeZ;
        return dx * dx + dz * dz <= NODE_RADIUS_SQ;
    }

    private static QteType parseQteType(String text) {
        if (text.contains("Luck")) return QteType.LUCK;
        if (text.contains("Exp")) return QteType.EXP;
        if (text.contains("Chance")) return QteType.CHANCE;
        if (text.contains("Coins")) return QteType.COINS;
        return null;
    }

    private static boolean isTypeEnabled(QteType type) {
        return switch (type) {
            case LUCK -> qteLuckEnabled;
            case EXP -> qteExpEnabled;
            case CHANCE -> qteChanceEnabled;
            case COINS -> qteCoinsEnabled;
            case TICK_SKIP -> qteTickSkipEnabled;
        };
    }

    private static boolean isColocatedXZ(Entity target, List<Entity> others) {
        for (Entity other : others) {
            if (Math.abs(target.getX() - other.getX()) <= POSITION_EPSILON
                    && Math.abs(target.getZ() - other.getZ()) <= POSITION_EPSILON) {
                return true;
            }
        }
        return false;
    }

    private static boolean isColocated(Entity target, List<Entity> others) {
        for (Entity other : others) {
            if (Math.abs(target.getX() - other.getX()) <= POSITION_EPSILON
                    && Math.abs(target.getY() - other.getY()) <= POSITION_EPSILON
                    && Math.abs(target.getZ() - other.getZ()) <= POSITION_EPSILON) {
                return true;
            }
        }
        return false;
    }

    public static void reset() {
        tracked = List.of();
        qteFirstSeenMs.clear();
        ambientBlacklist.clear();
        lastTrackedNodeUuid = null;
    }
}
