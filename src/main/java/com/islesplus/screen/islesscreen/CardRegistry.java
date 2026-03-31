package com.islesplus.screen.islesscreen;

import com.islesplus.IslesClient;
import com.islesplus.IslesPlusConfig;
import com.islesplus.features.autoparty.AutoParty;
import com.islesplus.features.bosstracker.BossaryHook;
import com.islesplus.features.bosstracker.BossTracker;
import com.islesplus.features.chatfilter.ChatFilter;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier;
import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.dropnotifier.DropNotifier;
import com.islesplus.features.inventorynotifier.InventoryNotifier;
import com.islesplus.sound.SoundConfig;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.harvesttimer.HarvestTimer;
import com.islesplus.features.harvesttimer.NerdModeActivator;
import com.islesplus.features.nodealertmanager.NodeAlertManager;
import com.islesplus.features.noderadius.NodeRadiusRenderer;
import com.islesplus.features.playerfinder.PlayerFinder;
import com.islesplus.features.rankcalculator.RankCalculator;
import com.islesplus.features.plushiefinder.PlushieFinder;
import com.islesplus.features.qtetracker.QteTracker;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.slotlocker.SlotLocker;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.sound.SoundController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class CardRegistry {
    private final List<FeatureCard> qolCards = new ArrayList<>();
    private final List<FeatureCard> nodeFarmingCards = new ArrayList<>();
    private final List<FeatureCard> riftCards = new ArrayList<>();
    private final FeatureCard keybindsCard = new FeatureCard(
        "Keybinds",
        "Available Isles+ keybinds",
        () -> Items.PAPER,
        () -> false,
        e -> {},
        null, null, null, null, null, null, false
    ).withKeybinds();


    // ==============================
    // QOL Cards
    // ==============================

    CardRegistry() {
        qolCards.add(new FeatureCard(
            "Inventory Full",
            "Notify when inventory is full",
            () -> Items.CHEST,
            () -> InventoryNotifier.inventoryFullNotifyEnabled,
            enabled -> { InventoryNotifier.inventoryFullNotifyEnabled = enabled; IslesPlusConfig.save(); },
            null, null, null,
            null, null, null, false
        ).withKilledKey("inventory_full")
         .withSoundConfig(
             () -> InventoryNotifier.soundConfig,
             v  -> InventoryNotifier.soundConfig = v,
             new SoundConfig("minecraft:block.chest.open", 0.62f, 0.95f)
         ));
        qolCards.add(new FeatureCard(
            "Mod-only Sounds",
            "Only hear mod-triggered audio",
            () -> Items.JUKEBOX,
            () -> IslesClient.modOnlySoundsEnabled,
            enabled -> { IslesClient.modOnlySoundsEnabled = enabled; SoundController.setModOnlySoundsEnabled(enabled); IslesPlusConfig.save(); }
        ));
        qolCards.add(new FeatureCard(
            "Plushie Finder",
            "Highlight plushies",
            () -> Items.CARROT_ON_A_STICK,
            () -> PlushieFinder.plushieFinderEnabled,
            enabled -> { PlushieFinder.plushieFinderEnabled = enabled; IslesPlusConfig.save(); }
        ).withKilledKey("plushie_finder"));
        qolCards.add(new FeatureCard(
            "Slot Locking",
            "Lock slots with L to prevent swaps",
            () -> Items.SHIELD,
            () -> SlotLocker.slotLockEnabled,
            enabled -> { SlotLocker.slotLockEnabled = enabled; IslesPlusConfig.save(); }
        ));
        qolCards.add(new FeatureCard(
            "Inventory QOL",
            "Search bar and calculator for inventories",
            () -> Items.COMPASS,
            () -> InventorySearch.inventorySearchEnabled,
            enabled -> { InventorySearch.inventorySearchEnabled = enabled; IslesPlusConfig.save(); },
            new String[]{ "Top Left", "Top Center", "Top Right", "Bottom Right", "Bottom Center", "Bottom Left" },
            new BooleanSupplier[]{
                () -> InventorySearch.barPosition == InventorySearch.SearchBarPosition.TOP_LEFT,
                () -> InventorySearch.barPosition == InventorySearch.SearchBarPosition.TOP_CENTER,
                () -> InventorySearch.barPosition == InventorySearch.SearchBarPosition.TOP_RIGHT,
                () -> InventorySearch.barPosition == InventorySearch.SearchBarPosition.BOTTOM_RIGHT,
                () -> InventorySearch.barPosition == InventorySearch.SearchBarPosition.BOTTOM_CENTER,
                () -> InventorySearch.barPosition == InventorySearch.SearchBarPosition.BOTTOM_LEFT
            },
            new Runnable[]{
                () -> { InventorySearch.barPosition = InventorySearch.SearchBarPosition.TOP_LEFT; IslesPlusConfig.save(); },
                () -> { InventorySearch.barPosition = InventorySearch.SearchBarPosition.TOP_CENTER; IslesPlusConfig.save(); },
                () -> { InventorySearch.barPosition = InventorySearch.SearchBarPosition.TOP_RIGHT; IslesPlusConfig.save(); },
                () -> { InventorySearch.barPosition = InventorySearch.SearchBarPosition.BOTTOM_RIGHT; IslesPlusConfig.save(); },
                () -> { InventorySearch.barPosition = InventorySearch.SearchBarPosition.BOTTOM_CENTER; IslesPlusConfig.save(); },
                () -> { InventorySearch.barPosition = InventorySearch.SearchBarPosition.BOTTOM_LEFT; IslesPlusConfig.save(); }
            },
            null, null, null, false
        ).withKilledKey("inventory_search").withLegend(
            "Use commas for AND search",
            "Prefix with # to search lore"
        ));
        qolCards.add(new FeatureCard(
            "Ground Items Notifier",
            "Highlight dropped items on the ground",
            () -> Items.GOLD_NUGGET,
            () -> GroundItemsNotifier.groundItemsNotifierEnabled,
            enabled -> { GroundItemsNotifier.groundItemsNotifierEnabled = enabled; IslesPlusConfig.save(); },
            null, null, null,
            null, null, null, false
        ).withItemList());
        FeatureCard bossCard = new FeatureCard(
            "Boss Timers",
            "Track world boss spawn timers",
            () -> Items.GHAST_TEAR,
            () -> BossTracker.bossTrackerEnabled,
            enabled -> {
                BossTracker.bossTrackerEnabled = enabled;
                if (enabled && BossTracker.autoOpenBossary) {
                    BossTracker.pendingBossaryOpen = true;
                }
                IslesPlusConfig.save();
            },
            null, null, null,
            null, null, null, false
        ).withKilledKey("boss_tracker").withBossList();
        qolCards.add(bossCard);
        qolCards.add(new FeatureCard(
            "Auto Party",
            "One keybind to invite your party",
            () -> Items.PLAYER_HEAD,
            () -> AutoParty.enabled,
            enabled -> { AutoParty.enabled = enabled; IslesPlusConfig.save(); },
            null, null, null,
            null, null, null, false
        ).withAutoParty());
        qolCards.add(new FeatureCard(
            "Chat Filters",
            "Hide unwanted chat messages",
            () -> Items.BARRIER,
            () -> ChatFilter.chatFilterEnabled,
            enabled -> { ChatFilter.chatFilterEnabled = enabled; IslesPlusConfig.save(); },
            new String[]{"Mana Meteor", "Guild Chat"},
            new BooleanSupplier[]{
                () -> ChatFilter.filterManaMeteor,
                () -> ChatFilter.filterGuildChat
            },
            new Runnable[]{
                () -> { ChatFilter.filterManaMeteor = !ChatFilter.filterManaMeteor; IslesPlusConfig.save(); },
                () -> { ChatFilter.filterGuildChat  = !ChatFilter.filterGuildChat;  IslesPlusConfig.save(); }
            },
            null, null, null, false
        ).withKilledKey("chat_filter"));

        // ==============================
        // Node Farming Cards
        // ==============================

        nodeFarmingCards.add(new FeatureCard(
            "Node Radius",
            "Portal particle ring around active node",
            () -> Items.ENDER_PEARL,
            () -> NodeRadiusRenderer.nodeRadiusEnabled,
            enabled -> { NodeRadiusRenderer.nodeRadiusEnabled = enabled; IslesPlusConfig.save(); }
        ).withKilledKey("node_radius"));
        nodeFarmingCards.add(new FeatureCard(
            "Drop Notify",
            "Announce 8x/16x drops",
            () -> Items.ENDER_EYE,
            () -> DropNotifier.dropNotifyEnabled,
            enabled -> { DropNotifier.dropNotifyEnabled = enabled; IslesPlusConfig.save(); },
            null, null, null,
            null, null, null, false
        ).withKilledKey("drop_notify")
         .withSoundConfig(
             () -> DropNotifier.soundConfig,
             v  -> DropNotifier.soundConfig = v,
             new SoundConfig("minecraft:entity.ender_dragon.growl", 0.85f, 1.00f)
         ));
        nodeFarmingCards.add(new FeatureCard(
            "Node Depleted Ping",
            "Alert when current node depletes",
            () -> Items.BELL,
            () -> NodeAlertManager.depletionPingEnabled,
            enabled -> { NodeAlertManager.depletionPingEnabled = enabled; IslesPlusConfig.save(); },
            null, null, null,
            null, null, null, false
        ).withKilledKey("node_depleted_ping")
         .withSoundConfig(
             () -> NodeAlertManager.depletionSoundConfig,
             v  -> NodeAlertManager.depletionSoundConfig = v,
             new SoundConfig("minecraft:block.note_block.bass", 0.85f, 0.60f)
         ));
        nodeFarmingCards.add(new FeatureCard(
            "Regen Mode",
            "Ping when node regenerates",
            () -> Items.AMETHYST_SHARD,
            () -> NodeAlertManager.regenPingMode != NodeAlertManager.RegenPingMode.OFF,
            enabled -> {
                if (!enabled) {
                    NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.OFF;
                    NodeAlertManager.regenReminderActive = false;
                } else if (NodeAlertManager.regenPingMode == NodeAlertManager.RegenPingMode.OFF) {
                    NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT;
                }
                IslesPlusConfig.save();
            },
            new String[]{"Short Ping", "Ping-Until-Interact"},
            new BooleanSupplier[]{
                () -> NodeAlertManager.regenPingMode == NodeAlertManager.RegenPingMode.SHORT_PING,
                () -> NodeAlertManager.regenPingMode == NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT
            },
            new Runnable[]{
                () -> { NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.SHORT_PING;          NodeAlertManager.regenReminderActive = false; IslesPlusConfig.save(); },
                () -> { NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT;                                                IslesPlusConfig.save(); }
            },
            null, null, null, false
        ).withKilledKey("regen_mode")
         .withSoundConfig(
             () -> NodeAlertManager.regenSoundConfig,
             v  -> NodeAlertManager.regenSoundConfig = v,
             new SoundConfig("minecraft:block.note_block.bell", 1.00f, 1.00f)
         ));
        nodeFarmingCards.add(new FeatureCard(
            "Harvest Timer",
            "Auto-enables Nerd Mode via /settings",
            () -> Items.CLOCK,
            () -> HarvestTimer.harvestTimerEnabled,
            enabled -> {
                HarvestTimer.harvestTimerEnabled = enabled;
                IslesPlusConfig.save();
                if (enabled) NerdModeActivator.activate();
            },
            true
        ).withKilledKey("harvest_timer"));
        FeatureCard qteCard = new FeatureCard(
            "QTE Tracker",
            "Line + box on quick time events",
            () -> Items.FISHING_ROD,
            () -> QteTracker.qteTrackerEnabled,
            enabled -> { QteTracker.qteTrackerEnabled = enabled; IslesPlusConfig.save(); },
            new String[]{"Luck", "Exp", "Chance", "Coins", "Tick Skip"},
            new BooleanSupplier[]{
                () -> QteTracker.qteLuckEnabled,
                () -> QteTracker.qteExpEnabled,
                () -> QteTracker.qteChanceEnabled,
                () -> QteTracker.qteCoinsEnabled,
                () -> QteTracker.qteTickSkipEnabled
            },
            new Runnable[]{
                () -> { QteTracker.qteLuckEnabled     = !QteTracker.qteLuckEnabled;     IslesPlusConfig.save(); },
                () -> { QteTracker.qteExpEnabled      = !QteTracker.qteExpEnabled;      IslesPlusConfig.save(); },
                () -> { QteTracker.qteChanceEnabled   = !QteTracker.qteChanceEnabled;   IslesPlusConfig.save(); },
                () -> { QteTracker.qteCoinsEnabled    = !QteTracker.qteCoinsEnabled;    IslesPlusConfig.save(); },
                () -> { QteTracker.qteTickSkipEnabled = !QteTracker.qteTickSkipEnabled; IslesPlusConfig.save(); }
            },
            null, null, null, false
        );
        qteCard.note = "(Shaders may affect line rendering)";
        qteCard.killedKey = "qte_tracker";
        nodeFarmingCards.add(qteCard);

        // ==============================
        // Rift Cards
        // ==============================

        FeatureCard buttonFinder = new FeatureCard(
            "Button Finder",
            "Highlight buttons",
            () -> Items.SPYGLASS,
            () -> SecretFinder.secretFinderEnabled,
            riftToggle(enabled -> { SecretFinder.secretFinderEnabled = enabled; if (!enabled) SecretFinder.reset(); IslesPlusConfig.save(); }),
            null, null, null,
            "Color",
            () -> SecretFinder.glowHue,
            v  -> { SecretFinder.glowHue = v; },
            true
        );
        buttonFinder.note = "(Shaders may break this feature)";
        buttonFinder.killedKey = "button_finder";
        riftCards.add(buttonFinder);
        riftCards.add(new FeatureCard(
            "Chest Finder",
            "Highlight chests",
            () -> Items.COMPASS,
            () -> ChestFinder.chestFinderEnabled,
            riftToggle(enabled -> { ChestFinder.chestFinderEnabled = enabled; if (!enabled) ChestFinder.reset(); IslesPlusConfig.save(); }),
            null, null, null,
            "Color",
            () -> ChestFinder.glowHue,
            v  -> { ChestFinder.glowHue = v; },
            true
        ).withKilledKey("chest_finder"));
        riftCards.add(new FeatureCard(
            "Vending Machine Finder",
            "Highlight vending machines",
            () -> Items.EMERALD,
            () -> VendingMachineFinder.vendingMachineFinderEnabled,
            riftToggle(enabled -> { VendingMachineFinder.vendingMachineFinderEnabled = enabled; if (!enabled) VendingMachineFinder.reset(); IslesPlusConfig.save(); }),
            null, null, null,
            "Color",
            () -> VendingMachineFinder.glowHue,
            v  -> { VendingMachineFinder.glowHue = v; },
            true
        ).withKilledKey("vending_machine_finder"));
        riftCards.add(new FeatureCard(
            "Mob Finder",
            "Highlight nearby mobs",
            () -> Items.ZOMBIE_HEAD,
            () -> MobFinder.mobFinderEnabled,
            riftToggle(enabled -> { MobFinder.mobFinderEnabled = enabled; if (!enabled) MobFinder.reset(); IslesPlusConfig.save(); }),
            null, null, null,
            "Color",
            () -> MobFinder.glowHue,
            v  -> { MobFinder.glowHue = v; },
            true
        ).withKilledKey("mob_finder"));
        riftCards.add(new FeatureCard(
            "Player Highlight",
            "Highlight nearby players",
            () -> Items.PLAYER_HEAD,
            () -> PlayerFinder.playerFinderEnabled,
            riftToggle(enabled -> { PlayerFinder.playerFinderEnabled = enabled; if (!enabled) PlayerFinder.reset(); IslesPlusConfig.save(); }),
            null, null, null,
            "Color",
            () -> PlayerFinder.glowHue,
            v  -> { PlayerFinder.glowHue = v; },
            true
        ).withKilledKey("player_finder"));
        riftCards.add(new FeatureCard(
            "Score Calculator",
            "Live rank HUD from Rift scoreboard",
            () -> Items.NETHER_STAR,
            () -> RankCalculator.rankCalculatorEnabled,
            riftToggle(enabled -> { RankCalculator.rankCalculatorEnabled = enabled; IslesPlusConfig.save(); }),
            new String[]{"Show player count", "Show rank drop timer"},
            new BooleanSupplier[]{ () -> RankCalculator.showPlayerCount, () -> RankCalculator.showRankDropTimer },
            new Runnable[]{ () -> { RankCalculator.showPlayerCount = !RankCalculator.showPlayerCount; IslesPlusConfig.save(); },
                            () -> { RankCalculator.showRankDropTimer = !RankCalculator.showRankDropTimer; IslesPlusConfig.save(); } },
            null, null, null, false
        ).withKilledKey("rank_calculator").withLegend(
            "\u2620 Mob = 1 point",
            "\uD83C\uDF81 Chest = 5 points",
            "\uD83D\uDC51 Boss = 25 points"
        ));
    }

    /**
     * Wraps a rift card toggle callback to show a first-time warning before enabling.
     * Disabling always works without the warning.
     */
    private static Consumer<Boolean> riftToggle(Consumer<Boolean> apply) {
        return enabled -> {
            if (!enabled || RiftWarningManager.isDismissed()) {
                apply.accept(enabled);
            } else {
                MinecraftClient mc = MinecraftClient.getInstance();
                Screen current = mc.currentScreen;
                mc.setScreen(new RiftWarningScreen(current, () -> apply.accept(true)));
            }
        };
    }

    List<FeatureCard> getActiveCards(Tab tab) {
        List<FeatureCard> base = switch (tab) {
            case QOL -> qolCards;
            case NODE_FARMING -> nodeFarmingCards;
            case RIFT -> riftCards;
        };
        List<FeatureCard> result = new ArrayList<>(base);
        if (tab == Tab.QOL) result.add(keybindsCard);
        return result;
    }
}
