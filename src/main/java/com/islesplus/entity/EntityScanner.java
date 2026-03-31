package com.islesplus.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class EntityScanner {
    private static final double RADIUS_NEAR = 7.0;
    private static final double RADIUS_NEAR_DOUBLE = RADIUS_NEAR * 2.0;
    private static final double RADIUS_FAR = 120.0;
    private static final double RADIUS_PLAYERS = 256.0;
    private static final double RADIUS_NEAR_SQ = RADIUS_NEAR * RADIUS_NEAR;
    private static final double RADIUS_NEAR_DOUBLE_SQ = RADIUS_NEAR_DOUBLE * RADIUS_NEAR_DOUBLE;
    private static final double RADIUS_FAR_SQ = RADIUS_FAR * RADIUS_FAR;
    private static final double RADIUS_PLAYERS_SQ = RADIUS_PLAYERS * RADIUS_PLAYERS;

    private EntityScanner() {}

    public static EntityScanResult scan(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return EntityScanResult.EMPTY;
        }

        List<Entity> textDisplaysNear = new ArrayList<>();
        List<Entity> textDisplaysNearDouble = new ArrayList<>();
        List<Entity> interactions = new ArrayList<>();
        List<Entity> itemDisplays = new ArrayList<>();
        List<Entity> areaEffectClouds = new ArrayList<>();
        List<Entity> textDisplaysFar = new ArrayList<>();
        List<Entity> players = new ArrayList<>();
        List<Entity> slimes = new ArrayList<>();
        List<Entity> armorStands = new ArrayList<>();
        List<Entity> itemEntities = new ArrayList<>();

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            double distSq = client.player.squaredDistanceTo(entity);

            if (entity instanceof PlayerEntity && distSq <= RADIUS_PLAYERS_SQ) {
                players.add(entity);
            }

            if (distSq > RADIUS_FAR_SQ) continue;

            String type = entity.getType().toString();
            switch (type) {
                case "entity.minecraft.text_display" -> {
                    textDisplaysFar.add(entity);
                    if (distSq <= RADIUS_NEAR_DOUBLE_SQ) textDisplaysNearDouble.add(entity);
                    if (distSq <= RADIUS_NEAR_SQ) textDisplaysNear.add(entity);
                }
                case "entity.minecraft.interaction" -> interactions.add(entity);
                case "entity.minecraft.item_display" -> itemDisplays.add(entity);
                case "entity.minecraft.area_effect_cloud" -> areaEffectClouds.add(entity);
                case "entity.minecraft.slime" -> slimes.add(entity);
                case "entity.minecraft.armor_stand" -> {
                    if (distSq <= RADIUS_NEAR_SQ) armorStands.add(entity);
                }
                case "entity.minecraft.item" -> itemEntities.add(entity);
            }
        }

        return new EntityScanResult(
            textDisplaysNear,
            textDisplaysNearDouble,
            interactions,
            itemDisplays,
            areaEffectClouds,
            textDisplaysFar,
            players,
            slimes,
            armorStands,
            itemEntities
        );
    }
}
