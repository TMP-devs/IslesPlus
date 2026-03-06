package com.islesplus.features.secretfinder;

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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class SecretBlockRenderer {
    private static final float LINE_WIDTH = 3.0f;
    private static final float EXPAND = 0.002f;

    private static final RenderPipeline NO_DEPTH_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of("islesplus", "no_depth_lines"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );

    private static final RenderLayer NO_DEPTH_LINES_LAYER = RenderLayer.of(
        "islesplus_no_depth_lines",
        RenderSetup.builder(NO_DEPTH_LINES_PIPELINE).build()
    );

    private SecretBlockRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!SecretFinder.secretFinderEnabled || FeatureFlags.isKilled("button_finder")) return;
        var positions = SecretFinder.getSlimeBlockPositions();
        if (positions.isEmpty()) return;
        if (ctx.consumers() == null || ctx.matrices() == null) return;

        int color = 0xFF000000 | MathHelper.hsvToRgb(SecretFinder.glowHue, 1.0f, 1.0f);

        Camera cam = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d camPos = cam.getCameraPos();
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        VertexConsumer lines = consumers.getBuffer(NO_DEPTH_LINES_LAYER);

        float min = -EXPAND;
        float max = 1.0f + EXPAND;

        for (BlockPos pos : positions) {
            matrices.push();
            matrices.translate(
                pos.getX() - camPos.x,
                pos.getY() - camPos.y,
                pos.getZ() - camPos.z
            );
            MatrixStack.Entry entry = matrices.peek();

            // Bottom face (Y-min)
            edge(lines, entry, min, min, min, max, min, min, 1, 0, 0, color);
            edge(lines, entry, max, min, min, max, min, max, 0, 0, 1, color);
            edge(lines, entry, max, min, max, min, min, max, 1, 0, 0, color);
            edge(lines, entry, min, min, max, min, min, min, 0, 0, 1, color);
            // Top face (Y-max)
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
