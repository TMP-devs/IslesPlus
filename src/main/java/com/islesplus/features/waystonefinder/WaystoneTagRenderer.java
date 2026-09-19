package com.islesplus.features.waystonefinder;

import com.islesplus.render.WorldTagRenderer;
import com.islesplus.sync.FeatureFlags;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** name tags for waystones that are too far for their model to be loaded */
public final class WaystoneTagRenderer {
    private static final int TEXT = 0xFFFFFFFF;

    private WaystoneTagRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!WaystoneFinder.waystoneFinderEnabled || FeatureFlags.isKilled("waystone_finder")) return;
        List<WaystoneFinder.FarLabel> labels = WaystoneFinder.getFarLabels();
        if (labels.isEmpty()) return;
        if (ctx.consumers() == null || ctx.matrices() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Camera cam = client.gameRenderer.getCamera();
        Vec3d camPos = cam.getCameraPos();
        float yaw = cam.getYaw();
        float pitch = cam.getPitch();
        VertexConsumerProvider consumers = ctx.consumers();
        MatrixStack matrices = ctx.matrices();

        // only the nearest one, two tags at once is just noise
        WaystoneFinder.FarLabel closest = null;
        double best = Double.MAX_VALUE;
        for (WaystoneFinder.FarLabel label : labels) {
            double dx = label.x() - camPos.x;
            double dy = label.y() - camPos.y;
            double dz = label.z() - camPos.z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < best) {
                best = d;
                closest = label;
            }
        }
        if (closest == null) return;

        // darker version of the glow color so white text stays readable on it. Follows the colour
        // drawer's saturation and lightness too; at their defaults (1.0, 0.5) this is exactly the
        // old hue-only colour. Value is capped so the white text never sits on a pale background.
        float sat = 0.7f * MathHelper.clamp(WaystoneFinder.glowSaturation, 0f, 1f);
        float val = MathHelper.clamp(WaystoneFinder.glowLightness, 0.1f, 0.6f);
        int bg = 0xCC000000 | (MathHelper.hsvToRgb(WaystoneFinder.glowHue, sat, val) & 0xFFFFFF);

        WorldTagRenderer.drawTag(client, matrices, camPos, yaw, pitch,
            closest.x(), closest.y() + 1.0, closest.z(),
            closest.name() + " Waystone", TEXT, bg, 0.6f, consumers);

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }
}
