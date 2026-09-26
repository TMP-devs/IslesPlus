package com.islesplus.features.quickactions;

import com.islesplus.IslesClient;
import com.islesplus.features.autoparty.AutoParty;
import com.islesplus.features.inventorynotifier.InventoryNotifier;
import com.islesplus.features.resourcevault.ResourceVaultOpener;
import com.islesplus.screen.islesscreen.IslesScreen;
import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

/**
 * The Isles+ things a quick action button can do. Ids are what the config saves, so never rename
 * one; labels are free to change.
 */
public enum IslesAction {
    OPEN_SETTINGS("open_settings", "Open Isles+ settings", "minecraft:writable_book", null,
        c -> c.setScreen(new IslesScreen())),
    BACKPACK("backpack", "Open backpack (/bp)", "minecraft:bundle", null, c -> command(c, "bp")),
    TRASH("trash", "Open trash (/trash)", "minecraft:lava_bucket", null, c -> command(c, "trash")),
    RESOURCE_VAULT("resource_vault", "Open resource vault", "minecraft:chest", null,
        c -> ResourceVaultOpener.activate()),
    COSMETICS_HALL("cosmetics_hall", "Cosmetics hall (/cosmeticshall)", "minecraft:armor_stand", null,
        c -> command(c, "cosmeticshall")),
    AUTO_PARTY("auto_party", "Auto party", "minecraft:cake", "auto_party", c -> {
        if (AutoParty.enabled) AutoParty.trigger(c);
        else IslesClient.sendInfoMessage(c, "Auto Party is off. Turn it on in /ip first.");
    }),
    PARTY_WARP("party_warp", "Party warp (/p warp)", "minecraft:ender_pearl", "auto_party", c -> {
        if (AutoParty.enabled) command(c, "p warp");
        else IslesClient.sendInfoMessage(c, "Auto Party is off. Turn it on in /ip first.");
    }),
    CONFIRM_INVENTORY_FULL("confirm_inventory_full", "Confirm full inventory", "minecraft:barrel", "inventory_full",
        c -> InventoryNotifier.confirm());

    public final String id;
    public final String label;
    /** icon the button gets when the player picks this action and has not chosen one */
    public final String defaultIcon;
    /** features.json key that also blocks this action, null = only the quick_actions key */
    public final String killKey;
    private final Consumer<MinecraftClient> run;

    IslesAction(String id, String label, String defaultIcon, String killKey, Consumer<MinecraftClient> run) {
        this.id = id;
        this.label = label;
        this.defaultIcon = defaultIcon;
        this.killKey = killKey;
        this.run = run;
    }

    public boolean killed() {
        return killKey != null && FeatureFlags.isKilled(killKey);
    }

    /** "killed" in the json: left out of the action picker entirely */
    public boolean hidden() {
        return killKey != null && FeatureFlags.isHidden(killKey);
    }

    public void run(MinecraftClient client) {
        if (hidden()) {
            IslesClient.sendInfoMessage(client, label + " is not available.");
            return;
        }
        if (killed()) {
            String reason = FeatureFlags.disabledReason(killKey);
            IslesClient.sendInfoMessage(client, label + " is disabled right now." + (reason.isEmpty() ? "" : " " + reason));
            return;
        }
        run.accept(client);
    }

    public static IslesAction byId(String id) {
        for (IslesAction a : values()) if (a.id.equals(id)) return a;
        return null;
    }

    private static void command(MinecraftClient client, String command) {
        if (client.player != null) client.player.networkHandler.sendChatCommand(command);
    }
}
