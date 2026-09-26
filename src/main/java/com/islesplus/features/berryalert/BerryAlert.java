package com.islesplus.features.berryalert;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.entity.NodeSkill;
import com.islesplus.entity.TrackedNode;
import com.islesplus.features.nodealertmanager.NodeTracker;
import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Util;

import java.util.HashSet;
import java.util.Set;

/**
 * "Berry spawned!" on screen while farming, and the berries glow pink.
 * <p>
 * A berry is a farming QTE with no text of its own (found with {@code /ip scan}): an item_display
 * holding a player head, with a 1x1 interaction - the thing you click - on exactly the same spot,
 * a couple of blocks from the player. The "CLICK ME" bonus QTEs are armor stands wearing a head,
 * so they never match. Only on a farming node: mining has a QTE of its own (sulfur) that is not
 * this feature's business.
 * <p>
 * Five clicks pick a berry and the next one appears, so one round is many berries; the sound
 * plays once per round ({@link BerryRound}). The time left is the server's own ("LEFT 2.4s (1/5)")
 * and is not repeated.
 */
public final class BerryAlert {
    public static boolean berryAlertEnabled = true;
    public static SoundConfig soundConfig = new SoundConfig("minecraft:block.note_block.pling", 1.0f, 1.0f);

    private static final double POSITION_EPSILON = 0.01;
    /** Berries appear 2-3 blocks from the player; this leaves out a neighbour's. */
    private static final double MAX_PLAYER_DISTANCE_SQ = 6.0 * 6.0;
    private static final float TITLE_SCALE = 4f;

    private static final BerryRound round = new BerryRound();

    private BerryAlert() {}

    /** Ids of the berry heads up right now: they glow. */
    private static volatile Set<Integer> berryIds = Set.of();

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!berryAlertEnabled || client.player == null) {
            reset();
            return;
        }
        berryIds = farming() ? berriesNear(client, scan) : Set.of();
        if (round.update(!berryIds.isEmpty(), Util.getMeasuringTimeMs())) {
            ModSounds.playConfig(client, soundConfig);
        }
    }

    /** A berry round is on: the QTE tracker steps its tick skip marker aside for it. */
    public static boolean roundActive() {
        return berryAlertEnabled && round.active() && !FeatureFlags.isKilled("berry_alert");
    }

    public static boolean shouldForceGlow(Entity entity) {
        return berryAlertEnabled && entity != null && berryIds.contains(entity.getId())
            && !FeatureFlags.isKilled("berry_alert");
    }

    /** Pink, the QTE tracker's Luck colour, so a berry reads apart from the red title. */
    public static int glowRgb() {
        return 0xFF55FF;
    }

    private static final String TITLE = "Berry spawned!";

    /** "Berry spawned!" for the first 3s of a round, where a vanilla title sits by default. */
    public static final HudElement ELEMENT = new HudElement("berry_alert", "Berry Alert",
        new HudPlacement(HudAnchor.CENTER, HudAnchor.CENTER, 0, -22)) {
        @Override public boolean enabled() { return berryAlertEnabled; }
        @Override public boolean active(MinecraftClient client) {
            if (!round.active() || client.options.hudHidden) return false;
            // tick() stops running once the feature is remotely killed, so it cannot end the round
            if (!berryAlertEnabled || FeatureFlags.isKilled("berry_alert")) { reset(); return false; }
            return round.titleVisible(Util.getMeasuringTimeMs());
        }
        @Override public Size measure(boolean preview) {
            return new Size(Fonts.hudWidth(TITLE, TITLE_SCALE),
                Fonts.height(TITLE_SCALE) + Math.max(1, Math.round(TITLE_SCALE)));
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            Fonts.drawShadowed(ctx, TITLE, 0, 0, Theme.HUD_RED, TITLE_SCALE);
        }
    };

    public static void reset() {
        round.reset();
        berryIds = Set.of();
    }

    private static boolean farming() {
        TrackedNode node = NodeTracker.trackedNode;
        return WorldIdentification.world == PlayerWorld.ISLE
            && node != null
            && node.skill == NodeSkill.FARMING
            && NodeTracker.selfActivelyFarmingTrackedNode;
    }

    private static Set<Integer> berriesNear(MinecraftClient client, EntityScanResult scan) {
        Set<Integer> found = new HashSet<>();
        for (Entity display : scan.itemDisplays) {
            if (!(display instanceof DisplayEntity.ItemDisplayEntity item)) continue;
            if (display.squaredDistanceTo(client.player) > MAX_PLAYER_DISTANCE_SQ) continue;
            if (!item.getItemStack().isOf(Items.PLAYER_HEAD)) continue;
            for (Entity box : scan.interactions) {
                if (sameSpot(display.getX(), display.getY(), display.getZ(), box.getX(), box.getY(), box.getZ())) {
                    found.add(display.getId());
                    break;
                }
            }
        }
        return Set.copyOf(found);
    }

    static boolean sameSpot(double ax, double ay, double az, double bx, double by, double bz) {
        return Math.abs(ax - bx) <= POSITION_EPSILON
            && Math.abs(ay - by) <= POSITION_EPSILON
            && Math.abs(az - bz) <= POSITION_EPSILON;
    }
}
