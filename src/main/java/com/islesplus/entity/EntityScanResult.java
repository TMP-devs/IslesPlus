package com.islesplus.entity;

import net.minecraft.entity.Entity;

import java.util.List;

public final class EntityScanResult {
    /** TextDisplay entities within 7 blocks - for NodeTracker */
    public final List<Entity> textDisplaysNear;
    /** TextDisplay entities within 14 blocks - only for active-farming % chance checks */
    public final List<Entity> textDisplaysNearDouble;
    /** Interaction entities within 120 blocks - for PlushieFinder, SecretFinder */
    public final List<Entity> interactions;
    /** ItemDisplay entities within 120 blocks - for PlushieFinder, ChestFinder, MobFinder, SecretFinder */
    public final List<Entity> itemDisplays;
    /** AreaEffectCloud entities within 120 blocks - for ChestFinder, MobFinder */
    public final List<Entity> areaEffectClouds;
    /** TextDisplay entities within 120 blocks - used as filter input by PlushieFinder, ChestFinder, MobFinder */
    public final List<Entity> textDisplaysFar;
    /** Player entities within 256 blocks - for PlayerFinder */
    public final List<Entity> players;
    /** Slime entities within 120 blocks - for SecretFinder */
    public final List<Entity> slimes;
    /** ArmorStand entities within 7 blocks - for QteTracker */
    public final List<Entity> armorStands;
    /** Item (dropped) entities within 120 blocks - for GroundItemsNotifier */
    public final List<Entity> itemEntities;

    public EntityScanResult(
        List<Entity> textDisplaysNear,
        List<Entity> textDisplaysNearDouble,
        List<Entity> interactions,
        List<Entity> itemDisplays,
        List<Entity> areaEffectClouds,
        List<Entity> textDisplaysFar,
        List<Entity> players,
        List<Entity> slimes,
        List<Entity> armorStands,
        List<Entity> itemEntities
    ) {
        this.textDisplaysNear = textDisplaysNear;
        this.textDisplaysNearDouble = textDisplaysNearDouble;
        this.interactions = interactions;
        this.itemDisplays = itemDisplays;
        this.areaEffectClouds = areaEffectClouds;
        this.textDisplaysFar = textDisplaysFar;
        this.players = players;
        this.slimes = slimes;
        this.armorStands = armorStands;
        this.itemEntities = itemEntities;
    }

    public static final EntityScanResult EMPTY = new EntityScanResult(
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
    );
}
