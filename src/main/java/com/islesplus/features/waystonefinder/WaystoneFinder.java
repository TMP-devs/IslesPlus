package com.islesplus.features.waystonefinder;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import com.islesplus.sync.FeatureFlags;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WaystoneFinder {
    public static boolean waystoneFinderEnabled = true;
    public static float glowHue = 0.13f; // gold
    public static float glowSaturation = 1.0f;
    public static float glowLightness = 0.5f;

    /** Glow colour as 0xRRGGBB (no alpha). Defaults reproduce the old hsv(hue, 1, 1) colour. */
    public static int glowRgb() { return com.islesplus.ui.GlowColor.rgb(glowHue, glowSaturation, glowLightness); }

    /** a waystone whose model isn't loaded but whose name label still is */
    public record FarLabel(String name, double x, double y, double z) {}

    private static final Pattern LABEL = Pattern.compile("^(.+?) Waystone\\s*\\nClick to open the Menu!\\s*$");

    private static final Map<Integer, Boolean> modelCache = new HashMap<>();
    private static volatile Set<Integer> glowingEntityIds = Set.of();
    private static volatile List<FarLabel> farLabels = List.of();

    private WaystoneFinder() {}

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!waystoneFinderEnabled
                || WorldIdentification.world == PlayerWorld.RIFT
                || WorldIdentification.world == PlayerWorld.DISABLED_RIFT
                || client.player == null || client.world == null) {
            glowingEntityIds = Set.of();
            farLabels = List.of();
            modelCache.clear();
            return;
        }

        Set<Integer> next = new HashSet<>();
        Set<Integer> seenIds = new HashSet<>();
        List<Entity> models = new ArrayList<>();

        for (Entity display : scan.itemDisplays) {
            int id = display.getId();
            seenIds.add(id);
            Boolean cached = modelCache.get(id);
            boolean isWaystone;
            if (cached != null) {
                isWaystone = cached;
            } else {
                isWaystone = isWaystoneModel(display);
                modelCache.put(id, isWaystone);
            }
            if (isWaystone) {
                next.add(id);
                models.add(display);
            }
        }

        // the server culls the model entities way before the text label, so far away all we
        // have left is "<Name> Waystone". draw a tag there until the real thing loads in.
        // once any waystone is close enough to glow that's the one you're at, no tags at all
        List<FarLabel> labels = new ArrayList<>();
        if (models.isEmpty()) for (Entity entity : scan.textDisplaysFar) {
            if (!(entity instanceof DisplayEntity.TextDisplayEntity textDisplay)) continue;
            String text;
            try {
                text = textDisplay.getText().getString();
            } catch (Throwable ignored) {
                continue;
            }
            Matcher m = LABEL.matcher(text);
            if (!m.find()) continue;
            labels.add(new FarLabel(m.group(1).trim(), entity.getX(), entity.getY(), entity.getZ()));
        }

        modelCache.keySet().removeIf(key -> !seenIds.contains(key));
        glowingEntityIds = Set.copyOf(next);
        farLabels = List.copyOf(labels);
    }

    public static List<FarLabel> getFarLabels() {
        return farLabels;
    }

    // waystones are modelengine props, totem + halo both live under prop_waystone/
    private static boolean isWaystoneModel(Entity entity) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay)) return false;
        try {
            ItemStack stack = itemDisplay.getStackReference(0).get();
            if (stack.isEmpty()) return false;
            Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
            return model != null
                    && "modelengine".equals(model.getNamespace())
                    && model.getPath().startsWith("prop_waystone/");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void reset() {
        modelCache.clear();
        glowingEntityIds = Set.of();
        farLabels = List.of();
    }

    public static boolean shouldForceGlow(Entity entity) {
        // the remote check matters: a killed finder's tick stops, so its set keeps the last entities
        return waystoneFinderEnabled && !FeatureFlags.isKilled("waystone_finder") && entity != null && glowingEntityIds.contains(entity.getId());
    }
}
