package com.islesplus.features.qtetracker;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.islesplus.sync.FeatureFlags;
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
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class QteRenderer {
    private static final float LINE_WIDTH = 6.0f;
    private static final float BOX_HALF = 0.25f;
    private static final float BOX_HALF_LARGE = 0.35f;
    private static final float BOX_HALF_SMALL = 0.1f;
    private static final float BOX_Y_OFFSET_TICK_SKIP = 0.1f;
    private static final float BOX_Y_OFFSET_EXP_COINS = 0.875f;
    private static final float LINE_START_DIST = 1.0f;

    private static final RenderPipeline QTE_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of("islesplus", "qte_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );

    private static final RenderLayer QTE_LINES_LAYER = RenderLayer.of(
        "islesplus_qte_lines",
        RenderSetup.builder(QTE_LINES_PIPELINE).build()
    );

    private QteRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!QteTracker.qteTrackerEnabled || FeatureFlags.isKilled("qte_tracker")) return;
        List<QteTracker.TrackedQte> tracked = QteTracker.getTracked();
        if (tracked.isEmpty()) return;
        if (ctx.consumers() == null || ctx.matrices() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Camera cam = client.gameRenderer.getCamera();
        Vec3d camPos = cam.getCameraPos();
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        VertexConsumer lines = consumers.getBuffer(QTE_LINES_LAYER);

        // Compute look direction from camera yaw/pitch for line start point
        double yawRad = Math.toRadians(cam.getYaw());
        double pitchRad = Math.toRadians(cam.getPitch());
        float lookX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float lookY = (float) (-Math.sin(pitchRad));
        float lookZ = (float) (Math.cos(yawRad) * Math.cos(pitchRad));

        for (QteTracker.TrackedQte qte : tracked) {
            Entity entity = client.world.getEntityById(qte.entityId());
            if (entity == null) continue;

            double targetX = entity.getX() - camPos.x;
            double targetY = entity.getY() - camPos.y;
            double targetZ = entity.getZ() - camPos.z;
            double dist = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
            if (dist < 0.1) continue;

            int color = qte.type().color;
            MatrixStack.Entry entry;

            // Compute box size and position
            // Luck/Chance: top of box aligns with text_display Y, larger box
            // Exp/Coins: fixed offset above entity feet
            // Tick Skip: box at entity feet, small
            float half = switch (qte.type()) {
                case TICK_SKIP -> BOX_HALF_SMALL;
                case LUCK, CHANCE -> BOX_HALF_LARGE;
                default -> BOX_HALF;
            };
            double boxY;
            if ((qte.type() == QteTracker.QteType.LUCK || qte.type() == QteTracker.QteType.CHANCE)
                    && !Double.isNaN(qte.textDisplayY())) {
                boxY = (qte.textDisplayY() - camPos.y) - half;
            } else if (qte.type() == QteTracker.QteType.EXP || qte.type() == QteTracker.QteType.COINS) {
                boxY = targetY + BOX_Y_OFFSET_EXP_COINS;
            } else {
                boxY = targetY + BOX_Y_OFFSET_TICK_SKIP;
            }

            // Line from crosshair (1 block along look direction) to box center
            matrices.push();
            entry = matrices.peek();
            edge(lines, entry,
                lookX * LINE_START_DIST, lookY * LINE_START_DIST, lookZ * LINE_START_DIST,
                (float) targetX, (float) boxY, (float) targetZ,
                0, 1, 0, color);
            matrices.pop();

            // Box around QTE
            matrices.push();
            matrices.translate(targetX, boxY, targetZ);
            entry = matrices.peek();
            float min = -half;
            float max = half;
            // Bottom face
            edge(lines, entry, min, min, min, max, min, min, 1, 0, 0, color);
            edge(lines, entry, max, min, min, max, min, max, 0, 0, 1, color);
            edge(lines, entry, max, min, max, min, min, max, 1, 0, 0, color);
            edge(lines, entry, min, min, max, min, min, min, 0, 0, 1, color);
            // Top face
            edge(lines, entry, min, max, min, max, max, min, 1, 0, 0, color);
            edge(lines, entry, max, max, min, max, max, max, 0, 0, 1, color);
            edge(lines, entry, max, max, max, min, max, max, 1, 0, 0, color);
            edge(lines, entry, min, max, max, min, max, min, 0, 0, 1, color);
            // Verticals
            edge(lines, entry, min, min, min, min, max, min, 0, 1, 0, color);
            edge(lines, entry, max, min, min, max, max, min, 0, 1, 0, color);
            edge(lines, entry, max, min, max, max, max, max, 0, 1, 0, color);
            edge(lines, entry, min, min, max, min, max, max, 0, 1, 0, color);
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
