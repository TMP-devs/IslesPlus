package com.islesplus.features.harvestables;

import com.islesplus.render.WorldTagRenderer;
import com.islesplus.sync.FeatureFlags;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Name tags where a harvestable was seen but its entity is not loaded now, like the far waystone
 * tags: the glow needs the entity, the tag only needs the spot. The nearest few only. */
public final class HarvestableWaypointRenderer {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MAX_TAGS = 12;
    private static final double MAX_DIST_SQ = 160.0 * 160.0;

    private HarvestableWaypointRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!HarvestableHighlighter.enabled || !HarvestableHighlighter.waypoints
            || FeatureFlags.isKilled(HarvestableHighlighter.KILL_KEY)) return;
        List<HarvestableHighlighter.Waypoint> all = HarvestableHighlighter.unloadedWaypoints();
        if (all.isEmpty() || ctx.consumers() == null || ctx.matrices() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Camera cam = client.gameRenderer.getCamera();
        Vec3d camPos = cam.getCameraPos();
        List<HarvestableHighlighter.Waypoint> shown = new ArrayList<>();
        for (HarvestableHighlighter.Waypoint w : all) {
            if (!w.kind().active()) continue;
            if (dist2(w, camPos) <= MAX_DIST_SQ) shown.add(w);
        }
        if (shown.isEmpty()) return;
        shown.sort(Comparator.comparingDouble(w -> dist2(w, camPos)));
        if (shown.size() > MAX_TAGS) shown = shown.subList(0, MAX_TAGS);

        VertexConsumerProvider consumers = ctx.consumers();
        MatrixStack matrices = ctx.matrices();
        for (HarvestableHighlighter.Waypoint w : shown) {
            // the kind's glow colour, darkened so white text stays readable (same rule as the waystone tags)
            HarvestableHighlighter.Harvestable k = w.kind();
            float sat = 0.7f * MathHelper.clamp(k.saturation, 0f, 1f);
            float val = MathHelper.clamp(k.lightness, 0.1f, 0.6f);
            int bg = 0xCC000000 | (MathHelper.hsvToRgb(k.hue, sat, val) & 0xFFFFFF);
            WorldTagRenderer.drawTag(client, matrices, camPos, cam.getYaw(), cam.getPitch(),
                w.x(), w.y() + 0.6, w.z(), w.kind().label, TEXT, bg, 0.5f, consumers);
        }
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) immediate.draw();
    }

    private static double dist2(HarvestableHighlighter.Waypoint w, Vec3d p) {
        double dx = w.x() - p.x, dy = w.y() - p.y, dz = w.z() - p.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
