package com.islesplus.features.plushiefinder;

import com.islesplus.features.rankcalculator.RiftRepository;
import com.islesplus.render.WorldTagRenderer;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public final class PlushieWaypointRenderer {
    // Plushie: white text on dark pink
    private static final int P_TEXT = 0xFFFFFFFF;
    private static final int P_BG = 0xCC8B1A5C;
    // Entrance: white text on dark blue
    private static final int E_TEXT = 0xFFFFFFFF;
    private static final int E_BG = 0xCC1A4A8B;

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

        // range limit applies after picking the nearest, so it's still "nearest or nothing"
        if (PlushieFinder.maxDistance > 0) {
            double d = distSq(camPos, closest.xReal, closest.yReal, closest.zReal);
            if (closest.hasEntrance()) {
                d = Math.min(d, distSq(camPos, closest.xEntrance, closest.yEntrance, closest.zEntrance));
            }
            double max = PlushieFinder.maxDistance;
            if (d > max * max) return;
        }

        WorldTagRenderer.drawTag(client, matrices, camPos, yaw, pitch,
            closest.xReal, closest.yReal + 0.5, closest.zReal,
            "#" + closest.num, P_TEXT, P_BG, 0.75f, consumers);
        if (closest.hasEntrance()) {
            WorldTagRenderer.drawTag(client, matrices, camPos, yaw, pitch,
                closest.xEntrance, closest.yEntrance + 0.5, closest.zEntrance,
                "#" + closest.num + " entrance", E_TEXT, E_BG, 0.5f, consumers);
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static double distSq(Vec3d from, double x, double y, double z) {
        double dx = x - from.x;
        double dy = y - from.y;
        double dz = z - from.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
