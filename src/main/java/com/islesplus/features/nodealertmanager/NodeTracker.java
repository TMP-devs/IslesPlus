package com.islesplus.features.nodealertmanager;

import com.islesplus.IslesClient;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import com.islesplus.entity.EntityScanResult;
import com.islesplus.entity.NodeSnapshot;
import com.islesplus.entity.TrackedNode;
import com.islesplus.features.harvesttimer.HarvestTimer;
import com.islesplus.features.noderadius.NodeRadiusRenderer;
import com.islesplus.features.qtetracker.QteTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NodeTracker {
    private static final long NODE_STATE_COOLDOWN_MS = 2_500L;
    private static final long LOCK_DURATION_MS = 3_000L;
    private static final int COUNT_DROP_TO_ARM = 2;
    private static final double CHANCE_SIGNAL_MAX_DISTANCE_SQ = 9.0;

    // public so IslesScreen (com.islesplus.screen) can read/write
    public static boolean nodeUpdatesEnabled = false;
    public static TrackedNode trackedNode;
    public static boolean selfActivelyFarmingTrackedNode = false;
    public static long lastNodeTransitionAlertMs = 0L;

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (client.player == null || client.world == null) {
            clearTrackedNode(client, "left tracking range/world");
            trackedNode = null;
            selfActivelyFarmingTrackedNode = false;
            NodeAlertManager.regenReminderActive = false;
            NodeAlertManager.stopSkillAlert();
            return;
        }

        if (WorldIdentification.world != PlayerWorld.ISLE) {
            clearTrackedNode(client, "not in Isles world");
            trackedNode = null;
            selfActivelyFarmingTrackedNode = false;
            NodeAlertManager.regenReminderActive = false;
            NodeAlertManager.stopSkillAlert();
            return;
        }
        
        nodeUpdatesEnabled = NodeAlertManager.regenPingMode != NodeAlertManager.RegenPingMode.OFF
            || NodeAlertManager.depletionPingEnabled
            || NodeRadiusRenderer.nodeRadiusEnabled
            || HarvestTimer.harvestTimerEnabled
            || QteTracker.qteTrackerEnabled;
        
        if (!nodeUpdatesEnabled) {
            clearTrackedNode(client, "node updates disabled");
            trackedNode = null;
            selfActivelyFarmingTrackedNode = false;
            NodeAlertManager.regenReminderActive = false;
            NodeAlertManager.stopSkillAlert();
            return;
        }

        Entity chanceAnchor = findChanceSignalAnchor(scan.textDisplaysNear, trackedNode, client);
        NodeSnapshot snapshot;
        boolean chanceSignalInDoubleNear = selfActivelyFarmingTrackedNode
            && trackedNode != null
            && isSelfFarmingSignalForTrackedNode(scan.textDisplaysNearDouble, trackedNode);

        if (chanceSignalInDoubleNear && trackedNode != null) {
            // While actively farming with a live % chance signal, stay pinned to the current node.
            snapshot = findSnapshotByUuid(trackedNode.textDisplayUuid, client, scan.textDisplaysNear);
            if (snapshot == null) {
                // If the tracked node moved outside 7 blocks but is still within 14, keep it pinned.
                snapshot = findSnapshotByUuid(trackedNode.textDisplayUuid, client, scan.textDisplaysNearDouble);
            }
            if (snapshot == null) {
                // Keep current tracked node state while chance signal still confirms active farming.
                return;
            }
        } else if (chanceAnchor != null) {
            // When % chance is visible, prefer the node physically closest to that signal.
            snapshot = findNearestNodeSnapshotToAnchor(chanceAnchor, scan.textDisplaysNear);
        } else {
            snapshot = findNearestNodeSnapshot(client, scan);
        }
        long now = Util.getMeasuringTimeMs();
        if (snapshot == null) {
            clearTrackedNode(client, "no known node nearby");
            trackedNode = null;
            selfActivelyFarmingTrackedNode = false;
            NodeAlertManager.regenReminderActive = false;
            return;
        }

        if (trackedNode == null || !trackedNode.textDisplayUuid.equals(snapshot.uuid)) {
            if (trackedNode != null) {
                clearTrackedNode(client, "switched nodes");
            }
            trackedNode = new TrackedNode();
            trackedNode.resetFor(snapshot, now);
            selfActivelyFarmingTrackedNode = false;
            NodeAlertManager.regenReminderActive = false;
            IslesClient.sendStatusMessage(client, "Tracking candidate node: " + snapshot.nodeName);
            return;
        }

        // Keep position up-to-date in case the entity was re-added to the world
        trackedNode.nodeX = snapshot.entityX;
        trackedNode.nodeY = snapshot.entityY;
        trackedNode.nodeZ = snapshot.entityZ;

        boolean selfActiveNow = isSelfFarmingSignalForTrackedNode(scan.textDisplaysNearDouble, trackedNode);
        if (selfActiveNow != selfActivelyFarmingTrackedNode) {
            if (selfActiveNow) {
                IslesClient.sendStatusMessage(client, "You are now actively farming tracked node: " + trackedNode.nodeName);
            } else {
                IslesClient.sendStatusMessage(client, "You are no longer actively farming tracked node: " + trackedNode.nodeName);
            }
            selfActivelyFarmingTrackedNode = selfActiveNow;
        }

        // % chance is authoritative: immediately mark the node as active and armed.
        if (selfActiveNow) {
            trackedNode.sawSelfFarming = true;
            if (!trackedNode.locked) {
                trackedNode.locked = true;
                IslesClient.sendStatusMessage(client, "Tracking node: " + trackedNode.nodeName);
            }
            if (!trackedNode.armed) {
                trackedNode.armed = true;
                IslesClient.sendStatusMessage(client, "Node armed: " + trackedNode.nodeName);
            }
        }

        if (!trackedNode.locked && now - trackedNode.firstSeenMs >= LOCK_DURATION_MS) {
            trackedNode.locked = true;
            IslesClient.sendStatusMessage(client, "Tracking node: " + trackedNode.nodeName);
        }

        if (!trackedNode.locked) {
            return;
        }

        if (!trackedNode.armed) {
            if (snapshot.count >= 0) {
                trackedNode.peakCount = Math.max(trackedNode.peakCount, snapshot.count);
            }

            boolean sawCountDrop = trackedNode.peakCount >= 0
                && snapshot.count >= 0
                && trackedNode.peakCount - snapshot.count >= COUNT_DROP_TO_ARM;
            boolean depletedAfterObservedCount = snapshot.depleted && trackedNode.peakCount >= 0;
            boolean sawSelfFarmingSignal = trackedNode.sawSelfFarming;

            if (sawCountDrop || depletedAfterObservedCount || sawSelfFarmingSignal) {
                trackedNode.armed = true;
                IslesClient.sendStatusMessage(client, "Node armed: " + trackedNode.nodeName);
            }

            if (trackedNode.armed && NodeAlertManager.depletionPingEnabled && !trackedNode.wasDepleted && snapshot.depleted && now - lastNodeTransitionAlertMs >= NODE_STATE_COOLDOWN_MS) {
                NodeAlertManager.startSkillAlert(client, NodeAlertManager.ActiveSkillAlert.NODE_DEPLETED);
                lastNodeTransitionAlertMs = now;
            }

            trackedNode.previousCount = snapshot.count;
            trackedNode.wasDepleted = snapshot.depleted;
            if (!trackedNode.armed) {
                return;
            }
        }

        if (now - lastNodeTransitionAlertMs < NODE_STATE_COOLDOWN_MS) {
            trackedNode.previousCount = snapshot.count;
            trackedNode.wasDepleted = snapshot.depleted;
            return;
        }

        if (NodeAlertManager.depletionPingEnabled && !trackedNode.wasDepleted && snapshot.depleted) {
            NodeAlertManager.startSkillAlert(client, NodeAlertManager.ActiveSkillAlert.NODE_DEPLETED);
            lastNodeTransitionAlertMs = now;
        } else if (NodeAlertManager.regenPingMode != NodeAlertManager.RegenPingMode.OFF && trackedNode.wasDepleted && !snapshot.depleted) {
            lastNodeTransitionAlertMs = now;
            if (NodeAlertManager.regenPingMode == NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT && !selfActivelyFarmingTrackedNode) {
                NodeAlertManager.regenReminderActive = true;
                NodeAlertManager.lastRegenReminderMs = 0L;
                IslesClient.sendStatusMessage(client, "Regen reminder active for: " + trackedNode.nodeName);
            } else {
                NodeAlertManager.startSkillAlert(client, NodeAlertManager.ActiveSkillAlert.NODE_REGENERATED);
            }
        }

        trackedNode.previousCount = snapshot.count;
        trackedNode.wasDepleted = snapshot.depleted;
    }

    static NodeSnapshot findNearestNodeSnapshot(MinecraftClient client, EntityScanResult scan) {
        NodeSnapshot nearest = null;
        for (Entity entity : scan.textDisplaysNear) {
            if (!(entity instanceof DisplayEntity.TextDisplayEntity textDisplayEntity)) {
                continue;
            }
            double distanceSq = client.player.squaredDistanceTo(entity);
            String plain = textDisplayEntity.getText().getString();
            String normalized = normalizeNodeText(plain);
            String matchedNode = matchKnownNodeName(normalized);
            if (matchedNode == null) {
                continue;
            }
            boolean depleted = normalized.startsWith("depleted " + matchedNode.toLowerCase(Locale.ROOT));
            int count = extractLeadingCount(normalized);
            NodeSnapshot candidate = new NodeSnapshot(entity.getUuid(), matchedNode, normalized, count, depleted, distanceSq, entity.getX(), entity.getY(), entity.getZ());
            if (nearest == null || candidate.distanceSq < nearest.distanceSq) {
                nearest = candidate;
            }
        }
        return nearest;
    }

    static NodeSnapshot findNearestNodeSnapshotToAnchor(Entity anchor, List<Entity> textDisplays) {
        NodeSnapshot nearest = null;
        for (Entity entity : textDisplays) {
            if (!(entity instanceof DisplayEntity.TextDisplayEntity textDisplayEntity)) {
                continue;
            }
            String plain = textDisplayEntity.getText().getString();
            String normalized = normalizeNodeText(plain);
            String matchedNode = matchKnownNodeName(normalized);
            if (matchedNode == null) {
                continue;
            }
            boolean depleted = normalized.startsWith("depleted " + matchedNode.toLowerCase(Locale.ROOT));
            int count = extractLeadingCount(normalized);
            double dx = entity.getX() - anchor.getX();
            double dy = entity.getY() - anchor.getY();
            double dz = entity.getZ() - anchor.getZ();
            double distanceSq = dx * dx + dy * dy + dz * dz;
            NodeSnapshot candidate = new NodeSnapshot(entity.getUuid(), matchedNode, normalized, count, depleted, distanceSq, entity.getX(), entity.getY(), entity.getZ());
            if (nearest == null || candidate.distanceSq < nearest.distanceSq) {
                nearest = candidate;
            }
        }
        return nearest;
    }

    static NodeSnapshot findSnapshotByUuid(UUID uuid, MinecraftClient client, List<Entity> textDisplays) {
        for (Entity entity : textDisplays) {
            if (!entity.getUuid().equals(uuid)) {
                continue;
            }
            if (!(entity instanceof DisplayEntity.TextDisplayEntity textDisplayEntity)) {
                continue;
            }
            String plain = textDisplayEntity.getText().getString();
            String normalized = normalizeNodeText(plain);
            String matchedNode = matchKnownNodeName(normalized);
            if (matchedNode == null) {
                return null;
            }
            boolean depleted = normalized.startsWith("depleted " + matchedNode.toLowerCase(Locale.ROOT));
            int count = extractLeadingCount(normalized);
            double distanceSq = client.player.squaredDistanceTo(entity);
            return new NodeSnapshot(entity.getUuid(), matchedNode, normalized, count, depleted, distanceSq, entity.getX(), entity.getY(), entity.getZ());
        }
        return null;
    }

    static Entity findChanceSignalAnchor(List<Entity> textDisplays, TrackedNode trackedNode, MinecraftClient client) {
        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity entity : textDisplays) {
            if (!(entity instanceof DisplayEntity.TextDisplayEntity textDisplayEntity)) {
                continue;
            }
            String normalized = normalizeNodeText(textDisplayEntity.getText().getString());
            if (!normalized.contains("% chance")) {
                continue;
            }

            double distanceSq;
            if (trackedNode != null) {
                double dx = entity.getX() - trackedNode.nodeX;
                double dy = entity.getY() - trackedNode.nodeY;
                double dz = entity.getZ() - trackedNode.nodeZ;
                distanceSq = dx * dx + dy * dy + dz * dz;
            } else {
                distanceSq = client.player.squaredDistanceTo(entity);
            }

            if (distanceSq < bestDistSq) {
                best = entity;
                bestDistSq = distanceSq;
            }
        }
        return best;
    }

    static boolean isSelfFarmingSignalForTrackedNode(List<Entity> textDisplays, TrackedNode trackedNode) {
        if (trackedNode == null) {
            return false;
        }

        for (Entity entity : textDisplays) {
            if (!(entity instanceof DisplayEntity.TextDisplayEntity textDisplayEntity)) {
                continue;
            }
            String normalized = normalizeNodeText(textDisplayEntity.getText().getString());
            if (!normalized.contains("% chance")) {
                continue;
            }

            if (entity.getUuid().equals(trackedNode.textDisplayUuid)) {
                return true;
            }

            double dx = entity.getX() - trackedNode.nodeX;
            double dy = entity.getY() - trackedNode.nodeY;
            double dz = entity.getZ() - trackedNode.nodeZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq <= CHANCE_SIGNAL_MAX_DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }

    public static void clearTrackedNode(MinecraftClient client, String reason) {
        if (trackedNode == null) {
            return;
        }
        if (selfActivelyFarmingTrackedNode) {
            IslesClient.sendStatusMessage(client, "You are no longer actively farming tracked node: " + trackedNode.nodeName);
        }
        selfActivelyFarmingTrackedNode = false;
        NodeAlertManager.regenReminderActive = false;
        IslesClient.sendStatusMessage(client, "Stopped tracking node: " + trackedNode.nodeName + " (" + reason + ")");
    }

    public static String normalizeNodeText(String plainText) {
        return plainText
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    public static String matchKnownNodeName(String normalizedText) {
        for (String nodeName : NodeRepository.getNodeNames()) {
            if (normalizedText.contains(nodeName.toLowerCase(Locale.ROOT))) {
                return nodeName;
            }
        }
        return null;
    }

    static int extractLeadingCount(String normalizedText) {
        int xIndex = normalizedText.indexOf('x');
        if (xIndex <= 0) {
            return -1;
        }
        String maybeNumber = normalizedText.substring(0, xIndex).trim();
        int start = maybeNumber.length();
        while (start > 0 && Character.isDigit(maybeNumber.charAt(start - 1))) {
            start--;
        }
        if (start == maybeNumber.length()) {
            return -1;
        }
        try {
            return Integer.parseInt(maybeNumber.substring(start));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static void reset() {
        trackedNode = null;
        selfActivelyFarmingTrackedNode = false;
        lastNodeTransitionAlertMs = 0L;
    }
}
