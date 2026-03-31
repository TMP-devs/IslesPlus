package com.islesplus.features.grounditemsnotifier;

import com.islesplus.sync.FeatureFlags;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

public final class GroundItemsRenderer {
    private static final float LINE_WIDTH = 4.0f;
    private static final float BOX_HALF = 0.2f;
    private static final float LINE_START_DIST = 1.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of("islesplus", "ground_items_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );

    private static final RenderLayer LAYER = RenderLayer.of(
        "islesplus_ground_items_lines",
        RenderSetup.builder(PIPELINE).build()
    );

    private GroundItemsRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!GroundItemsNotifier.groundItemsNotifierEnabled || FeatureFlags.isKilled("ground_items_notifier")) return;
        Map<Integer, GroundItemsNotifier.WatchedItem> entityMap = GroundItemsNotifier.getMatchingEntityMap();
        if (entityMap.isEmpty()) return;
        if (ctx.consumers() == null || ctx.matrices() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        int color = 0xFF000000 | MathHelper.hsvToRgb(GroundItemsNotifier.glowHue, 1.0f, 1.0f);
        Camera cam = client.gameRenderer.getCamera();
        Vec3d camPos = cam.getCameraPos();
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        VertexConsumer lines = consumers.getBuffer(LAYER);

        double yawRad = Math.toRadians(cam.getYaw());
        double pitchRad = Math.toRadians(cam.getPitch());
        float lookX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float lookY = (float) (-Math.sin(pitchRad));
        float lookZ = (float) (Math.cos(yawRad) * Math.cos(pitchRad));

        for (Map.Entry<Integer, GroundItemsNotifier.WatchedItem> entry : entityMap.entrySet()) {
            if (!entry.getValue().lineTracker) continue;
            Entity entity = client.world.getEntityById(entry.getKey());
            if (entity == null) continue;

            double targetX = entity.getX() - camPos.x;
            double targetY = entity.getY() - camPos.y + 0.125;
            double targetZ = entity.getZ() - camPos.z;
            double dist = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
            if (dist < 0.1) continue;

            MatrixStack.Entry matEntry;

            // Line from crosshair to item
            matrices.push();
            matEntry = matrices.peek();
            edge(lines, matEntry,
                lookX * LINE_START_DIST, lookY * LINE_START_DIST, lookZ * LINE_START_DIST,
                (float) targetX, (float) targetY, (float) targetZ,
                0, 1, 0, color);
            matrices.pop();

            // Box around item
            matrices.push();
            matrices.translate(targetX, targetY, targetZ);
            matEntry = matrices.peek();
            float min = -BOX_HALF, max = BOX_HALF;
            // Bottom face
            edge(lines, matEntry, min, min, min, max, min, min, 1, 0, 0, color);
            edge(lines, matEntry, max, min, min, max, min, max, 0, 0, 1, color);
            edge(lines, matEntry, max, min, max, min, min, max, 1, 0, 0, color);
            edge(lines, matEntry, min, min, max, min, min, min, 0, 0, 1, color);
            // Top face
            edge(lines, matEntry, min, max, min, max, max, min, 1, 0, 0, color);
            edge(lines, matEntry, max, max, min, max, max, max, 0, 0, 1, color);
            edge(lines, matEntry, max, max, max, min, max, max, 1, 0, 0, color);
            edge(lines, matEntry, min, max, max, min, max, min, 0, 0, 1, color);
            // Verticals
            edge(lines, matEntry, min, min, min, min, max, min, 0, 1, 0, color);
            edge(lines, matEntry, max, min, min, max, max, min, 0, 1, 0, color);
            edge(lines, matEntry, max, min, max, max, max, max, 0, 1, 0, color);
            edge(lines, matEntry, min, min, max, min, max, max, 0, 1, 0, color);
            matrices.pop();
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static void edge(VertexConsumer vc, MatrixStack.Entry entry,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float nx, float ny, float nz, int color) {
        vc.vertex(entry, x1, y1, z1).color(color).normal(entry, nx, ny, nz).lineWidth(LINE_WIDTH);
        vc.vertex(entry, x2, y2, z2).color(color).normal(entry, nx, ny, nz).lineWidth(LINE_WIDTH);
    }
}
