package com.islesplus.features.treasurechest;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.render.WorldTagRenderer;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Waypoints on sunken chests: "◆ Sunken Tortuga Chest · 42m" over each one the client can see,
 * through walls and water, in its rarity's colour ({@link TreasureChest}). "TREASURE", locked and
 * unlabelled chests get none. Isles only - Rift crates are
 * Chest Finder's. A looted chest leaves the world, and its waypoint with it; up close
 * ({@link TreasureChest#HIDE_WITHIN}) the tag steps aside.
 */
public final class TreasureChestFinder {
    public static boolean treasureChestsEnabled = true;

    /** The server's own label sits half a block over the model; the tag goes above that. */
    private static final double TAG_ABOVE = 1.5;
    /** Smaller than the plushie (0.75) and waystone (0.6) tags: a chest is a label, not a sign. */
    private static final float TAG_SCALE = 0.5f;

    private record Chest(double x, double y, double z, String rarity, String name) {}

    /** The label stands on the chest's column, half a block over the model. */
    private static final double SAME_COLUMN = 0.1, LABEL_ABOVE_MAX = 1.5;

    private static volatile List<Chest> chests = List.of();

    private TreasureChestFinder() {}

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!treasureChestsEnabled || WorldIdentification.world != PlayerWorld.ISLE || client.player == null) {
            chests = List.of();
            return;
        }
        List<Chest> found = new ArrayList<>();
        for (Entity display : scan.itemDisplays) {
            if (!(display instanceof DisplayEntity.ItemDisplayEntity item)) continue;
            Identifier model = item.getItemStack().get(DataComponentTypes.ITEM_MODEL);
            if (model == null) continue;
            String rarity = TreasureChest.rarityOf(model.getNamespace(), model.getPath());
            if (rarity == null) continue;
            String name = nameOver(display, scan);
            if (name != null) found.add(new Chest(display.getX(), display.getY(), display.getZ(), rarity, name));
        }
        chests = List.copyOf(found);
    }

    /** The waypoint name from the label over this chest; null when it has none, or one that is
     * not a sunken chest (a locked one, say). */
    private static String nameOver(Entity chest, EntityScanResult scan) {
        for (Entity e : scan.textDisplaysFar) {
            if (!(e instanceof DisplayEntity.TextDisplayEntity label)) continue;
            double dy = e.getY() - chest.getY();
            if (Math.abs(e.getX() - chest.getX()) > SAME_COLUMN || Math.abs(e.getZ() - chest.getZ()) > SAME_COLUMN
                    || dy < 0 || dy > LABEL_ABOVE_MAX) continue;
            String name = TreasureChest.nameFromLabel(label.getText().getString());
            if (name != null) return name;
        }
        return null;
    }

    public static void render(WorldRenderContext ctx) {
        if (!treasureChestsEnabled || FeatureFlags.isKilled("treasure_chests")) return;
        List<Chest> now = chests;
        if (now.isEmpty() || ctx.consumers() == null || ctx.matrices() == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Camera cam = client.gameRenderer.getCamera();
        for (Chest chest : now) {
            double metres = Math.sqrt(client.player.squaredDistanceTo(chest.x(), chest.y(), chest.z()));
            if (!TreasureChest.showTag(metres) || !TreasureChest.shownFromHeight(chest.name(), client.player.getY())) continue;
            int color = TreasureChest.colorOf(chest.rarity());
            MutableText tag = Text.empty()
                .append(Fonts.symbol(Fonts.DIAMOND).withColor(color & 0xFFFFFF))
                .append(Text.literal(" " + chest.name()).withColor(color & 0xFFFFFF))
                .append(Text.literal(" · " + TreasureChest.distance(metres)));
            WorldTagRenderer.drawTag(client, ctx.matrices(), cam.getCameraPos(), cam.getYaw(), cam.getPitch(),
                chest.x(), chest.y() + TAG_ABOVE, chest.z(),
                tag, Theme.HUD_TEXT, TreasureChest.backgroundOf(chest.rarity()), TAG_SCALE, ctx.consumers());
        }
        if (ctx.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    public static void reset() {
        chests = List.of();
    }
}
