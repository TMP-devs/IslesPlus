package com.islesplus.features.harvestables;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.logging.IslesLog;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.GlowColor;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Glows the gatherable things lying around the Isles (fibers, apples, mushrooms, kelp...) through
 * walls, like the chest finder does in rifts. Each one can be switched on or off.
 *
 * <p>Server shape (scans 2026-09-24): a harvestable is an {@code interaction} plus a display at the
 * exact same spot - an {@code item_display} (the item shown: a MythicMobs "type" in its custom data,
 * or a plain vanilla model) or a {@code block_display}. The shared spot is what tells it apart from
 * everything else that uses displays; the item or block tells which harvestable it is.
 */
public final class HarvestableHighlighter {
    public static final String KILL_KEY = "harvestable_highlighter";

    /** Each kind has its own glow colour and its own remote kill key ("harvestable_<name>"). */
    public enum Harvestable {
        WHISPERLEAF("Whisperleaf", 0.33f),
        PLAIN_FIBER("Plain Fiber", 0.14f),
        COARSE_FIBER("Coarse Fiber", 0.08f),
        APPLE("Apple", 0.0f),
        MUSHROOM("Mushroom", 0.06f),
        KELP("Kelp", 0.45f),
        TANGLEWOOD_VINE("Tanglewood Vine", 0.25f),
        DRIFTSTONE("Driftstone", 0.58f),
        COBWEB("Cobweb", 0.78f);

        public final String label;
        public final float defaultHue;
        public float hue, saturation = 1.0f, lightness = 0.5f;

        Harvestable(String label, float hue) {
            this.label = label;
            this.defaultHue = hue;
            this.hue = hue;
        }

        /** features_v2.json key: a kill/disable there turns this one kind off. */
        public String key() { return "harvestable_" + name().toLowerCase(Locale.ROOT); }

        public int rgb() { return GlowColor.rgb(hue, saturation, lightness); }

        /** Highlighted: the player has it on and the json has not switched it off. */
        public boolean active() { return !hidden.contains(this) && !FeatureFlags.isKilled(key()); }
    }

    public static boolean enabled = false;
    /** Tags where a harvestable was seen but its entity is not loaded now (too far, culled). */
    public static boolean waypoints = true;
    /** Types the player switched off. Everything else is highlighted. */
    public static final Set<Harvestable> hidden = EnumSet.noneOf(Harvestable.class);

    /** Entity id -> what it is (null = looked at, not a harvestable). Cleared as entities leave. */
    private static final Map<Integer, Harvestable> cache = new HashMap<>();
    /** Entity id -> the kind glowing there this tick (its colour comes from the kind). */
    private static volatile Map<Integer, Harvestable> glowing = Map.of();
    /** A harvestable seen earlier: its tag is drawn while its entity is not loaded. */
    public record Waypoint(Harvestable kind, double x, double y, double z) {}

    /** Every harvestable seen since joining, by spot. Forgotten when you come close and it is gone
     * (picked, or it moved), since that close its entity would be loaded if it were there. */
    private static final Map<Long, Waypoint> remembered = new HashMap<>();
    /** Remembered spots whose entity is not loaded this tick: the ones that get a tag. */
    private static volatile java.util.List<Waypoint> unloaded = java.util.List.of();
    /** Within this distance a missing harvestable is taken as gone, not just unloaded. */
    private static final double FORGET_RADIUS_SQ = 8.0 * 8.0;

    /** Shapes that look like a harvestable but matched nothing, logged once each so they can be added. */
    private static final Set<String> loggedUnknown = new HashSet<>();

    private HarvestableHighlighter() {}

    /** The glow colour for this entity: its kind's colour. */
    public static int glowRgb(Entity entity) {
        Harvestable kind = entity == null ? null : glowing.get(entity.getId());
        return kind == null ? 0xFFFFFF : kind.rgb();
    }

    public static boolean shouldForceGlow(Entity entity) {
        return enabled && entity != null && glowing.containsKey(entity.getId()) && !FeatureFlags.isKilled(KILL_KEY);
    }

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!enabled || WorldIdentification.world != PlayerWorld.ISLE || client.player == null || client.world == null) {
            glowing = Map.of();
            cache.clear();
            remembered.clear();
            unloaded = java.util.List.of();
            return;
        }

        // Every harvestable sits on an interaction entity: index their spots.
        Set<Long> interactionSpots = new HashSet<>();
        for (Entity e : scan.interactions) interactionSpots.add(spot(e));

        Map<Integer, Harvestable> next = new HashMap<>();
        Set<Integer> seen = new HashSet<>();
        Set<Long> existingSpots = new HashSet<>();
        Set<Long> drawnSpots = new HashSet<>();
        for (Entity e : scan.itemDisplays) consider(e, client, interactionSpots, seen, next, existingSpots, drawnSpots);
        for (Entity e : scan.blockDisplaysFar) consider(e, client, interactionSpots, seen, next, existingSpots, drawnSpots);

        cache.keySet().removeIf(id -> !seen.contains(id));
        glowing = Map.copyOf(next);

        double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
        java.util.List<Waypoint> tags = new java.util.ArrayList<>();
        remembered.entrySet().removeIf(en -> {
            if (drawnSpots.contains(en.getKey())) return false;   // on screen and glowing: no tag
            Waypoint w = en.getValue();
            double dx = w.x() - px, dy = w.y() - py, dz = w.z() - pz;
            boolean exists = existingSpots.contains(en.getKey());
            if (!exists && dx * dx + dy * dy + dz * dz <= FORGET_RADIUS_SQ) return true;   // right here and gone: picked
            tags.add(w);
            return false;
        });
        unloaded = java.util.List.copyOf(tags);
    }

    /** Remembered harvestables whose entity is not loaded now, for the waypoint tags (all kinds;
     * the renderer skips the hidden ones). */
    public static java.util.List<Waypoint> unloadedWaypoints() { return unloaded; }

    private static void consider(Entity e, MinecraftClient client, Set<Long> interactionSpots, Set<Integer> seen,
                                 Map<Integer, Harvestable> next, Set<Long> existingSpots, Set<Long> drawnSpots) {
        if (!interactionSpots.contains(spot(e))) return;
        int id = e.getId();
        seen.add(id);
        Harvestable kind;
        if (cache.containsKey(id)) {
            kind = cache.get(id);
        } else {
            kind = classify(e);
            cache.put(id, kind);
        }
        if (kind == null) return;
        long key = spot(e);
        existingSpots.add(key);
        // "Drawn" = actually rendered. A display entity is sent to us well before it is rendered: the
        // server gives it a short view range, and until we are inside it there is nothing to glow,
        // so its waypoint tag stays up until then (the game's own render-distance check).
        if (e.shouldRender(e.squaredDistanceTo(client.gameRenderer.getCamera().getCameraPos()))) drawnSpots.add(key);
        remembered.put(key, new Waypoint(kind, e.getX(), e.getY(), e.getZ()));
        if (kind.active()) next.put(id, kind);
    }

    /** Position to 1/100 of a block, packed into one key: the display and its interaction share it exactly. */
    private static long spot(Entity e) {
        long x = Math.round(e.getX() * 100), y = Math.round(e.getY() * 100), z = Math.round(e.getZ() * 100);
        return (x * 73856093L) ^ (y * 19349663L) ^ (z * 83492791L);
    }

    private static Harvestable classify(Entity e) {
        try {
            if (e instanceof DisplayEntity.ItemDisplayEntity item) {
                ItemStack stack = item.getStackReference(0).get();
                if (stack.isEmpty()) return null;   // the chop rig's empty display
                Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
                String modelId = model == null ? "" : model.toString();
                String type = mythicType(stack);
                Harvestable kind = classifyItem(type, modelId);
                if (kind == null && !knownOther(modelId)) logUnknown("item_display model=" + modelId + " type=" + type);
                return kind;
            }
            if (e instanceof DisplayEntity.BlockDisplayEntity block) {
                String blockId = Registries.BLOCK.getId(block.getBlockState().getBlock()).toString();
                Harvestable kind = classifyBlock(blockId);
                if (kind == null) logUnknown("block_display block=" + blockId);
                return kind;
            }
        } catch (RuntimeException ignored) {
            // an odd entity must never break the tick
        }
        return null;
    }

    private static String mythicType(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return "";
        NbtCompound nbt = data.copyNbt();
        return nbt.getCompound("PublicBukkitValues")
            .flatMap(v -> v.getString("mythicmobs:type"))
            .orElse("");
    }

    /** Things that sit on an interaction but are never harvestables: no point logging them. */
    private static boolean knownOther(String model) {
        return model.startsWith("modelengine:") || model.startsWith("isles:plushies/") || model.startsWith("isles:cosmetics/")
            || model.startsWith("isles:effects/") || model.startsWith("isles:furniture/");
    }

    private static void logUnknown(String what) {
        if (loggedUnknown.add(what)) IslesLog.runtimeInfo("[Isles+] harvestable highlighter: unrecognised " + what);
    }

    /** Which harvestable an item display shows, from its MythicMobs type or its model; null = none.
     * Model engine parts (mobs, props) never count. */
    static Harvestable classifyItem(String mythicType, String model) {
        String type = mythicType == null ? "" : mythicType.toLowerCase(Locale.ROOT);
        String m = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (m.startsWith("modelengine:")) return null;
        switch (type) {
            case "material_whisperleaf" -> { return Harvestable.WHISPERLEAF; }
            case "material_plain_fiber" -> { return Harvestable.PLAIN_FIBER; }
            case "material_fiber" -> { return Harvestable.COARSE_FIBER; }
            case "gathering_apple" -> { return Harvestable.APPLE; }
            default -> { }
        }
        String both = type + " " + m;
        if (both.contains("whisperleaf")) return Harvestable.WHISPERLEAF;
        if (both.contains("plain_string") || both.contains("plain_fiber")) return Harvestable.PLAIN_FIBER;
        if (both.contains("coarse_string") || both.contains("coarse_fiber")) return Harvestable.COARSE_FIBER;
        if (both.contains("apple")) return Harvestable.APPLE;
        if (both.contains("mushroom")) return Harvestable.MUSHROOM;
        if (both.contains("kelp")) return Harvestable.KELP;
        if (both.contains("tanglewood") || both.contains("vine")) return Harvestable.TANGLEWOOD_VINE;
        if (both.contains("driftstone")) return Harvestable.DRIFTSTONE;
        if (both.contains("cobweb")) return Harvestable.COBWEB;
        return null;
    }

    /** Which harvestable a block display shows, from its block id; null = none. */
    static Harvestable classifyBlock(String blockId) {
        String b = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        if (b.contains("kelp") || b.contains("seagrass")) return Harvestable.KELP;
        // Tanglewood Vine is drawn as a jungle sapling (seen 2026-09-25)
        if (b.contains("vine") || b.equals("minecraft:jungle_sapling")) return Harvestable.TANGLEWOOD_VINE;
        // Driftstone is drawn as cobbled deepslate (seen 2026-09-25)
        if (b.contains("driftstone") || b.equals("minecraft:cobbled_deepslate")) return Harvestable.DRIFTSTONE;
        if (b.contains("mushroom")) return Harvestable.MUSHROOM;
        if (b.contains("cobweb")) return Harvestable.COBWEB;
        return null;
    }

    public static void reset() {
        cache.clear();
        glowing = Map.of();
        remembered.clear();
        unloaded = java.util.List.of();
    }
}
