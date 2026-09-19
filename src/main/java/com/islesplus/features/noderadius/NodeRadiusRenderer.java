package com.islesplus.features.noderadius;

import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import com.islesplus.features.nodealertmanager.NodeTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;

public class NodeRadiusRenderer {
    public static boolean nodeRadiusEnabled = false;

    private static final double RADIUS = 10.0;
    private static final int STEPS = 40;
    // Blocks above nodeY to start the floor search from
    private static final int FLOOR_SEARCH_ABOVE = 3;
    // Blocks below nodeY to search before giving up
    private static final int FLOOR_SEARCH_BELOW = 12;

    public static void tick(MinecraftClient client) {
        if (!nodeRadiusEnabled) return;
        if (WorldIdentification.world != PlayerWorld.ISLE) return;
        if (!NodeTracker.selfActivelyFarmingTrackedNode) return;
        if (NodeTracker.trackedNode == null) return;
        if (client.world == null) return;

        ClientWorld world = client.world;
        double centerX = NodeTracker.trackedNode.nodeX;
        double centerZ = NodeTracker.trackedNode.nodeZ;
        int baseY = (int) Math.floor(NodeTracker.trackedNode.nodeY);

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int i = 0; i < STEPS; i++) {
            double angle = 2.0 * Math.PI * i / STEPS;
            double px = centerX + RADIUS * Math.cos(angle);
            double pz = centerZ + RADIUS * Math.sin(angle);

            int blockX = (int) Math.floor(px);
            int blockZ = (int) Math.floor(pz);

            int floorY = findFloorY(world, mutable, blockX, baseY, blockZ);
            if (floorY == Integer.MIN_VALUE) continue;

            client.particleManager.addParticle(ParticleTypes.PORTAL, px, floorY + 0.1, pz, 0.0, 0.05, 0.0);
        }
    }

    /**
     * walks down the column at (x, z) starting a bit above baseY and returns the y of
     * the first air block sitting on something solid, aka the floor nearest the node.
     * works in caves, on the surface, multi floor builds etc. Integer.MIN_VALUE if nothing
     */
    private static int findFloorY(ClientWorld world, BlockPos.Mutable mutable, int x, int baseY, int z) {
        for (int y = baseY + FLOOR_SEARCH_ABOVE; y >= baseY - FLOOR_SEARCH_BELOW; y--) {
            mutable.set(x, y, z);
            if (!world.isAir(mutable)) continue;
            mutable.set(x, y - 1, z);
            if (!world.isAir(mutable)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static void reset() {
        // No mutable state to clear
    }
}
