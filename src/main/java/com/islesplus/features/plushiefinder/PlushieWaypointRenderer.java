package com.islesplus.features.plushiefinder;

import com.islesplus.features.rankcalculator.RiftRepository;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class PlushieWaypointRenderer {
    // Plushie: white text on dark pink
    private static final int P_TEXT = 0xFFFFFFFF;
    private static final int P_BG = 0xCC8B1A5C;
    // Entrance: white text on dark blue
    private static final int E_TEXT = 0xFFFFFFFF;
    private static final int E_BG = 0xCC1A4A8B;
    private static final double MAX_TAG_RENDER_DISTANCE = 160.0;

    private PlushieWaypointRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!PlushieFinder.plushieFinderEnabled || FeatureFlags.isKilled("plushie_finder")) return;
        boolean isIsle = WorldIdentification.world == PlayerWorld.ISLE;
        boolean isPlushieRift = (WorldIdentification.world == PlayerWorld.RIFT || WorldIdentification.world == PlayerWorld.DISABLED_RIFT) && RiftRepository.getPlushieRifts().contains(WorldIdentification.currentRiftName);
        if (!isIsle && !isPlushieRift) return;
        if (ctx.consumers() == null || ctx.matrices() == null) return;
        if (PlushieRepository.getCachedPlushies().isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Camera cam = client.gameRenderer.getCamera();
        Vec3d camPos = cam.getCameraPos();
        float yaw = cam.getYaw();
        float pitch = cam.getPitch();

        VertexConsumerProvider consumers = ctx.consumers();
        MatrixStack matrices = ctx.matrices();

        PlushieEntry closest = PlushieRepository.getClosestUnowned(camPos);
        if (closest == null) return;

        drawTag(client, matrices, camPos, yaw, pitch,
            closest.xReal, closest.yReal + 0.5, closest.zReal,
            "#" + closest.num, P_TEXT, P_BG, 0.75f, consumers);
        if (closest.hasEntrance()) {
            drawTag(client, matrices, camPos, yaw, pitch,
                closest.xEntrance, closest.yEntrance + 0.5, closest.zEntrance,
                "#" + closest.num + " entrance", E_TEXT, E_BG, 0.5f, consumers);
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    // Billboard using separate Y/X Euler rotations.
    private static void applyBillboard(MatrixStack matrices, float yaw, float pitch) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
    }

    private static void drawTag(MinecraftClient client, MatrixStack matrices, Vec3d camPos,
                                float yaw, float pitch,
                                double wx, double wy, double wz,
                                String text, int textColor, int bgColor,
                                float scaleMult, VertexConsumerProvider consumers) {
        double dx = wx - camPos.x;
        double dy = wy - camPos.y;
        double dz = wz - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-6) return;

        if (dist > MAX_TAG_RENDER_DISTANCE) {
            double s = MAX_TAG_RENDER_DISTANCE / dist;
            dx *= s;
            dy *= s;
            dz *= s;
        }

        // VoxelMap formula (dist*0.1+1)*constant; keep larger readable tags.
        float scale = (float) (dist * 0.1 + 1.0) * 0.045f * scaleMult;
        // Keep far labels readable, but bounded so they do not explode in size.
        scale = MathHelper.clamp(scale, 0.08f * scaleMult, 1.5f * scaleMult);

        matrices.push();
        matrices.translate(dx, dy, dz);
        applyBillboard(matrices, yaw, pitch);
        matrices.scale(-scale, -scale, -scale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int w = client.textRenderer.getWidth(text);
        client.textRenderer.draw(text, -w / 2.0f, 0, textColor, false, matrix,
            consumers, TextRenderer.TextLayerType.SEE_THROUGH, bgColor,
            LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
    }
}
