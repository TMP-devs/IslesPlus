package com.islesplus.features.qtetracker;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.entity.NodeSkill;
import com.islesplus.entity.TrackedNode;
import com.islesplus.features.nodealertmanager.NodeTracker;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

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

    /** boxYOffset/boxHalf only used for tick skip, block_display rigs sit a bit higher than item ones */
    public record TrackedQte(int entityId, QteType type, double textDisplayY, double boxYOffset, float boxHalf) {
        public TrackedQte(int entityId, QteType type, double textDisplayY) {
            this(entityId, type, textDisplayY, 0.1, 0.1f);
        }
    }

    // only woodcutting gets a box around its tick skip. mining/farming block_displays are scaled
    // all over the place so a box either covers it or floats way above it, and fishing's hopper
    // minigame doesn't want anything drawn over the fish either. those three just get the line
    private static final double BLOCK_TICK_SKIP_ABOVE = 0.3;
    private static final double ITEM_TICK_SKIP_ABOVE = 0.1;
    private static final float WOODCUTTING_TICK_SKIP_HALF = 0.18f;
    private static final float NO_BOX = 0f;

    public static boolean qteTrackerEnabled = true;

    public static boolean qteLuckEnabled = true;
    public static boolean qteExpEnabled = false;
    public static boolean qteChanceEnabled = true;
    public static boolean qteCoinsEnabled = false;
    public static boolean qteTickSkipEnabled = false;

    private static volatile List<TrackedQte> tracked = List.of();
    private static final Map<Integer, Long> qteFirstSeenMs = new HashMap<>();
    private static final Map<Integer, Long> tickSkipLastConfirmedMs = new HashMap<>();
    private static final Map<Integer, Double> tickSkipBlockDy = new HashMap<>();
    private static final Set<Integer> ambientBlacklist = new HashSet<>();
    private static UUID lastTrackedNodeUuid = null;
    private static final long QTE_MAX_AGE_MS = 10_000L;
    private static final long TICK_SKIP_GRACE_MS = 1_000L;

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

        // not tracking a node right now, so anything nearby is ambient stuff (auction house,
        // shops etc). blacklist them for good so they never get mistaken for a qte later
        if (node == null) {
            for (Entity interaction : scan.interactions) {
                ambientBlacklist.add(interaction.getId());
            }
            tracked = List.of();
            return;
        }

        // switched nodes, wipe the qte history
        UUID currentNodeUuid = node.textDisplayUuid;
        if (!currentNodeUuid.equals(lastTrackedNodeUuid)) {
            qteFirstSeenMs.clear();
            tickSkipLastConfirmedMs.clear();
            tickSkipBlockDy.clear();
            lastTrackedNodeUuid = currentNodeUuid;
        }

        long now = System.currentTimeMillis();

        // start the first-seen clock on nearby entities as soon as we're tracking the node,
        // so stuff that was already sitting there ages out before farming even starts
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

        // bonus qtes: "CLICK ME" text displays near the node, with an armor stand under them
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

            // find the armor stand at the same x/z sitting below the text
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

        // tick skip: one interaction entity that lives the whole minigame and hops around the node.
        // what's sitting on top of it depends on the skill:
        //   woodcutting / fishing -> item_display with an actual item (lime concrete, logs, fish...)
        //   mining                -> block_display (the green->red box you click at the last second)
        // the chop rig also has an interaction but its item_display is EMPTY, that's how we tell
        // them apart. right after you click there's a moment where the item is gone but the
        // interaction is still there, so we keep it around for a bit instead of flickering.
        // modelengine mobs (crabs, tortoises...) also have an interaction with item_display bones on it,
        // but they always have an area effect cloud right under them and their models are modelengine:
        if (isTypeEnabled(QteType.TICK_SKIP)) {
            // the node label says which skill it is, so only look for that skill's rig shape.
            // stops a farming crop box next to a tree getting picked up while woodcutting etc.
            // unknown skill (label didn't say) falls back to accepting either
            boolean wantBlock = node.skill == null || node.skill.usesBlockTickSkip();
            boolean wantItem = node.skill == null || !node.skill.usesBlockTickSkip();

            for (Entity interaction : scan.interactions) {
                int id = interaction.getId();
                if (!withinNodeRadius(interaction, node)) continue;
                if (ambientBlacklist.contains(id)) continue;
                if (hasAreaEffectCloudBelow(interaction, scan.areaEffectClouds)) continue;
                if (isColocatedXZWithKnownNode(interaction, scan.textDisplaysFar)) continue;

                double blockDy = wantBlock ? colocatedBlockDisplayDy(interaction, scan.blockDisplays) : Double.NaN;
                boolean block = !Double.isNaN(blockDy);
                if (block || (wantItem && hasColocatedItem(interaction, scan.itemDisplays))) {
                    tickSkipLastConfirmedMs.put(id, now);
                    tickSkipBlockDy.put(id, block ? blockDy : Double.NaN);
                    next.add(tickSkip(id, block ? blockDy : Double.NaN, node.skill));
                } else {
                    Long lastConfirmed = tickSkipLastConfirmedMs.get(id);
                    if (lastConfirmed != null && now - lastConfirmed < TICK_SKIP_GRACE_MS) {
                        Double dy = tickSkipBlockDy.get(id);
                        next.add(tickSkip(id, dy == null ? Double.NaN : dy, node.skill));
                    }
                }
            }
        } else {
            tickSkipLastConfirmedMs.clear();
            tickSkipBlockDy.clear();
        }

        tracked = List.copyOf(next);
    }

    // blockDy = how far above the interaction the block_display sits (NaN = item rig)
    private static TrackedQte tickSkip(int id, double blockDy, NodeSkill skill) {
        if (!Double.isNaN(blockDy)) {
            return new TrackedQte(id, QteType.TICK_SKIP, Double.NaN, blockDy + BLOCK_TICK_SKIP_ABOVE, NO_BOX);
        }
        float half = skill == NodeSkill.FISHING ? NO_BOX : WOODCUTTING_TICK_SKIP_HALF;
        return new TrackedQte(id, QteType.TICK_SKIP, Double.NaN, ITEM_TICK_SKIP_ABOVE, half);
    }

    private static boolean hasColocatedItem(Entity interaction, List<Entity> itemDisplays) {
        for (Entity other : itemDisplays) {
            if (!(other instanceof DisplayEntity.ItemDisplayEntity itemDisplay)) continue;
            if (Math.abs(interaction.getX() - other.getX()) > POSITION_EPSILON
                    || Math.abs(interaction.getY() - other.getY()) > POSITION_EPSILON
                    || Math.abs(interaction.getZ() - other.getZ()) > POSITION_EPSILON) continue;
            try {
                ItemStack stack = itemDisplay.getStackReference(0).get();
                if (stack.isEmpty()) continue;
                Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
                if (model != null && "modelengine".equals(model.getNamespace())) continue;
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    // mining puts the block_display exactly on the interaction, farming floats it 0.25 above.
    // returns that offset, or NaN if there's no block_display on this interaction
    private static double colocatedBlockDisplayDy(Entity interaction, List<Entity> blockDisplays) {
        for (Entity other : blockDisplays) {
            if (Math.abs(interaction.getX() - other.getX()) > POSITION_EPSILON
                    || Math.abs(interaction.getZ() - other.getZ()) > POSITION_EPSILON) continue;
            double dy = other.getY() - interaction.getY();
            if (dy >= -POSITION_EPSILON && dy <= 0.5) return Math.max(0, dy);
        }
        return Double.NaN;
    }

    // same x/z and within a block below or at the interaction's feet
    private static boolean hasAreaEffectCloudBelow(Entity interaction, List<Entity> clouds) {
        for (Entity cloud : clouds) {
            if (Math.abs(interaction.getX() - cloud.getX()) > POSITION_EPSILON
                    || Math.abs(interaction.getZ() - cloud.getZ()) > POSITION_EPSILON) continue;
            double dy = interaction.getY() - cloud.getY();
            if (dy >= -POSITION_EPSILON && dy <= 1.0) return true;
        }
        return false;
    }

    // the "+10 Ticks!" popup spawns where the tick skip just was, so only actual node labels count here
    private static boolean isColocatedXZWithKnownNode(Entity target, List<Entity> textDisplays) {
        for (Entity other : textDisplays) {
            if (Math.abs(target.getX() - other.getX()) > POSITION_EPSILON
                    || Math.abs(target.getZ() - other.getZ()) > POSITION_EPSILON) continue;
            if (!(other instanceof DisplayEntity.TextDisplayEntity textDisplay)) continue;
            String text;
            try {
                text = textDisplay.getText().getString();
            } catch (Throwable ignored) {
                continue;
            }
            if (NodeTracker.matchKnownNodeName(NodeTracker.normalizeNodeText(text)) != null) return true;
        }
        return false;
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

    // each type has its own remote kill key on top of the big qte_tracker one,
    // so we can turn off just tick skip if the server changes it again
    private static boolean isTypeEnabled(QteType type) {
        return switch (type) {
            case LUCK -> qteLuckEnabled && !FeatureFlags.isKilled("qte_tracker_luck");
            case EXP -> qteExpEnabled && !FeatureFlags.isKilled("qte_tracker_exp");
            case CHANCE -> qteChanceEnabled && !FeatureFlags.isKilled("qte_tracker_chance");
            case COINS -> qteCoinsEnabled && !FeatureFlags.isKilled("qte_tracker_coins");
            case TICK_SKIP -> qteTickSkipEnabled && !FeatureFlags.isKilled("qte_tracker_tickskip");
        };
    }

    public static void reset() {
        tracked = List.of();
        qteFirstSeenMs.clear();
        tickSkipLastConfirmedMs.clear();
        tickSkipBlockDy.clear();
        ambientBlacklist.clear();
        lastTrackedNodeUuid = null;
    }
}
