package com.islesplus.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** floating billboard text tag at a world position, shared by the waypoint style renderers */
public final class WorldTagRenderer {
    private static final double MAX_TAG_RENDER_DISTANCE = 160.0;

    private WorldTagRenderer() {}

    public static void drawTag(MinecraftClient client, MatrixStack matrices, Vec3d camPos,
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

        // stole the (dist*0.1+1)*constant scaling from voxelmap, keeps tags readable
        float scale = (float) (dist * 0.1 + 1.0) * 0.045f * scaleMult;
        // far ones stay readable but capped so they don't get massive
        scale = MathHelper.clamp(scale, 0.08f * scaleMult, 1.5f * scaleMult);

        matrices.push();
        matrices.translate(dx, dy, dz);
        // make it face the camera
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.scale(-scale, -scale, -scale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int w = client.textRenderer.getWidth(text);
        client.textRenderer.draw(text, -w / 2.0f, 0, textColor, false, matrix,
            consumers, TextRenderer.TextLayerType.SEE_THROUGH, bgColor,
            LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
    }
}
