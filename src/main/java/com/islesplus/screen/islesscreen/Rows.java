package com.islesplus.screen.islesscreen;

import com.islesplus.IslesClient;
import com.islesplus.IslesPlusConfig;
import com.islesplus.features.chatfilter.ChatFilter;
import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.dropnotifier.DropNotifier;
import com.islesplus.features.harvesttimer.HarvestTimer;
import com.islesplus.features.harvesttimer.NerdModeActivator;
import com.islesplus.features.inventorynotifier.InventoryNotifier;
import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.nodealertmanager.NodeAlertManager;
import com.islesplus.features.noderadius.NodeRadiusRenderer;
import com.islesplus.features.playerfinder.PlayerFinder;
import com.islesplus.features.plushiefinder.PlushieFinder;
import com.islesplus.features.plushiefinder.PlushieRepository;
import com.islesplus.features.qtetracker.QteTracker;
import com.islesplus.features.rankcalculator.RankCalculator;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.slotlocker.SlotLocker;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.features.waystonefinder.WaystoneFinder;
import com.islesplus.screen.islesscreen.rows.AutoPartyRow;
import com.islesplus.screen.islesscreen.rows.BossTimersRow;
import com.islesplus.screen.islesscreen.rows.GlowColorDrawer;
import com.islesplus.screen.islesscreen.rows.GroundItemsRow;
import com.islesplus.screen.islesscreen.rows.KeybindsRow;
import com.islesplus.sound.SoundConfig;
import com.islesplus.sound.SoundController;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.AnchorGrid;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.CheckTile;
import com.islesplus.ui.widgets.FootnoteBox;
import com.islesplus.ui.widgets.InfoChip;
import com.islesplus.ui.widgets.Rule;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.ValueSlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds the {@link FeatureRow} list for each tab, in the documented display order.
 *
 * <p>Three rows still to come from the {@code feat/profile-viewer} branch are marked with a
 * {@code //} comment at the exact index they belong at, so adding one is a single
 * {@code rows.add(...)} line inserted where the marker sits. Nothing else is placeheld: every
 * other row here is final.
 */
final class Rows {
    /** Horizontal/vertical gap inside drawer wrap-rows and tile grids. */
    private static final int ROW_GAP = 4;
    /** A tile grid drops to a single column below this width. */
    private static final int TILE_TWO_COL_MIN_WIDTH = 150;

    private Rows() {}

    // ==============================
    // QOL
    // ==============================

    static List<FeatureRow> qol(OverlayHost host) {
        List<FeatureRow> rows = new ArrayList<>();
        rows.add(inventoryFull(host));
        rows.add(new FeatureRow("Mod-only Sounds", "Only hear mod-triggered audio.")
            .toggle(() -> IslesClient.modOnlySoundsEnabled, v -> {
                IslesClient.modOnlySoundsEnabled = v;
                SoundController.setModOnlySoundsEnabled(v);
                IslesPlusConfig.save();
            })
            .beta());
        rows.add(plushieFinder());
        rows.add(finderRow("Waystone Finder", "Highlight waystones.", "waystone_finder", false,
            () -> WaystoneFinder.waystoneFinderEnabled,
            v -> { WaystoneFinder.waystoneFinderEnabled = v; if (!v) WaystoneFinder.reset(); IslesPlusConfig.save(); },
            () -> WaystoneFinder.glowHue, v -> WaystoneFinder.glowHue = v,
            () -> WaystoneFinder.glowSaturation, v -> WaystoneFinder.glowSaturation = v,
            () -> WaystoneFinder.glowLightness, v -> WaystoneFinder.glowLightness = v));
        rows.add(new FeatureRow("Slot Locking", "Lock slots with L to prevent swaps.")
            .toggle(() -> SlotLocker.slotLockEnabled,
                v -> { SlotLocker.slotLockEnabled = v; IslesPlusConfig.save(); }));
        rows.add(inventorySearch());
        rows.add(GroundItemsRow.build(host));
        rows.add(BossTimersRow.build(host));
        rows.add(AutoPartyRow.build(host));
        rows.add(chatFilters());
        // Profile Viewer row (after feat/profile-viewer merges)
        rows.add(KeybindsRow.build());
        return rows;
    }

    /** Plushie Finder: a range slider. All the way left means no limit ("MAX"); otherwise the
     * nearest plushie is only tagged within 5-200 blocks. */
    /** Remote kill switch (features.json) for the "Plushies" section under the range slider. Like
     * every other key there: true hides it, false (or missing) shows it. */
    private static final String PLUSHIE_RESET_KILL = "plushie_reset_button";

    private static FeatureRow plushieFinder() {
        int labelW = Fonts.labelColumn(Fonts.SMALL, "RANGE", "PLUSHIES");
        Widget range = new Flow.WrapRow(ROW_GAP, ROW_GAP)
            .add(new Label("RANGE", Theme.TEXT_LABEL, Fonts.SMALL).fixed(labelW))
            .add(new ValueSlider(PlushieFinder::maxDistanceSlider, PlushieFinder::setMaxDistanceFromSlider,
                0f, 1f, 0f, IslesPlusConfig::save))
            .add(new InfoChip(() -> PlushieFinder.maxDistance <= 0 ? "MAX" : PlushieFinder.maxDistance + " BLOCKS")
                .fixed(Fonts.width("200 BLOCKS", Fonts.SMALL) + 2 * Metrics.CHIP_PAD_X));

        Rule rule = new Rule();
        Button refresh = new Button("REFRESH PLUSHIES", Button.Kind.PRIMARY, PlushieRepository::refreshRemoteDataNowAsync);
        Widget data = new Flow.WrapRow(ROW_GAP, ROW_GAP)
            .add(new Label("PLUSHIES", Theme.TEXT_LABEL, Fonts.SMALL).fixed(labelW))
            .add(new InfoChip(() -> PlushieRepository.isRefreshing() ? "SYNCING..."
                    : PlushieRepository.getCachedPlushies().size() + " SYNCED")
                // Short on purpose: label + chip + button must share one line of the drawer. The label
                // already says "plushie", and the chip is sized for its longest text.
                .fixed(Fonts.width("SYNCING...", Fonts.SMALL) + 2 * Metrics.CHIP_PAD_X)
                .height(Metrics.BUTTON_H))   // level with the button beside it
            .add(refresh);

        // The section is hidden while its remote kill switch is on; checked on every layout so it
        // appears or disappears as soon as a refreshed features.json says so.
        Widget drawer = new Flow.Column(ROW_GAP + 2) {
            @Override public int layout(int x, int y, int width) {
                boolean show = !FeatureFlags.isKilled(PLUSHIE_RESET_KILL);
                rule.visible = show;
                data.visible = show;
                return super.layout(x, y, width);
            }
        }.add(range).add(rule).add(data);

        return new FeatureRow("Plushie Finder", "Highlight plushies.")
            .note("Open every /plushies page once to sync")
            .killedKey("plushie_finder")
            .toggle(() -> PlushieFinder.plushieFinderEnabled,
                v -> { PlushieFinder.plushieFinderEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static FeatureRow inventoryFull(OverlayHost host) {
        SoundConfig defaults = new SoundConfig("minecraft:block.chest.open", 0.62f, 0.95f);
        Widget drawer = SoundSection.of(host, () -> InventoryNotifier.soundConfig,
            v -> InventoryNotifier.soundConfig = v, defaults);
        return new FeatureRow("Inventory Full", "Notify when inventory is full.")
            .killedKey("inventory_full")
            .toggle(() -> InventoryNotifier.inventoryFullNotifyEnabled,
                v -> { InventoryNotifier.inventoryFullNotifyEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static FeatureRow inventorySearch() {
        AnchorGrid grid = new AnchorGrid(3,
            new String[]{"TOP LEFT", "TOP CENTER", "TOP RIGHT", "BOTTOM LEFT", "BOTTOM CENTER", "BOTTOM RIGHT"},
            () -> InventorySearch.barPosition.ordinal(),
            i -> {
                InventorySearch.barPosition = InventorySearch.SearchBarPosition.values()[i];
                IslesPlusConfig.save();
            });
        Widget drawer = new Flow.Column(Metrics.DRAWER_GAP)
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(new Label("PLACEMENT", Theme.TEXT_LABEL, Fonts.BODY))
                .add(new InfoChip(grid::selectedLabel)))
            .add(grid)
            // Help text: same outlined footnote box as every other card's hints (e.g. Boss Timers).
            .add(new FootnoteBox("AND - comma between terms", "LORE - prefix with #"));
        return new FeatureRow("Inventory Search", "Search bar and calculator for inventories.")
            .killedKey("inventory_search")
            .toggle(() -> InventorySearch.inventorySearchEnabled,
                v -> { InventorySearch.inventorySearchEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static FeatureRow chatFilters() {
        Widget drawer = new Flow.Column(Metrics.DRAWER_GAP)
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(new Label("HIDDEN TYPES", Theme.TEXT_LABEL, Fonts.BODY))
                .add(new InfoChip(() -> RowsText.countChip(hiddenChatTypes(), 3))))
            .add(tileGrid(
                tile("Mana Meteor", () -> ChatFilter.filterManaMeteor,
                    () -> ChatFilter.filterManaMeteor = !ChatFilter.filterManaMeteor),
                tile("Guild Chat", () -> ChatFilter.filterGuildChat,
                    () -> ChatFilter.filterGuildChat = !ChatFilter.filterGuildChat),
                tile("Player Deaths", () -> ChatFilter.filterDeaths,
                    () -> ChatFilter.filterDeaths = !ChatFilter.filterDeaths)))
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(new Button("SELECT ALL", Button.Kind.SECONDARY, () -> setAllChatFilters(true)).small())
                .add(new Button("REMOVE ALL", Button.Kind.SECONDARY, () -> setAllChatFilters(false)).small())
                .add(new Label(() -> RowsText.chatFilterSummary(hiddenChatTypes()), Theme.TEXT_META, Fonts.SMALL)));
        return new FeatureRow("Chat Filters", "Hide unwanted chat messages.")
            .killedKey("chat_filter")
            .toggle(() -> ChatFilter.chatFilterEnabled,
                v -> { ChatFilter.chatFilterEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static int hiddenChatTypes() {
        return count(() -> ChatFilter.filterManaMeteor, () -> ChatFilter.filterGuildChat, () -> ChatFilter.filterDeaths);
    }

    private static void setAllChatFilters(boolean on) {
        ChatFilter.filterManaMeteor = on;
        ChatFilter.filterGuildChat = on;
        ChatFilter.filterDeaths = on;
        IslesPlusConfig.save();
    }

    // ==============================
    // Node Farming
    // ==============================

    static List<FeatureRow> nodeFarming(OverlayHost host) {
        List<FeatureRow> rows = new ArrayList<>();
        rows.add(new FeatureRow("Node Radius", "Portal particle ring around active node.")
            .killedKey("node_radius")
            .toggle(() -> NodeRadiusRenderer.nodeRadiusEnabled,
                v -> { NodeRadiusRenderer.nodeRadiusEnabled = v; IslesPlusConfig.save(); }));
        rows.add(dropNotify(host));
        rows.add(nodeDepletedPing(host));
        rows.add(regenMode(host));
        rows.add(new FeatureRow("Harvest Timer", "Accurate time until your node is depleted.")
            .note("Auto-enables Nerd Mode via /settings")
            .killedKey("harvest_timer")
            .toggle(() -> HarvestTimer.harvestTimerEnabled, v -> {
                HarvestTimer.harvestTimerEnabled = v;
                IslesPlusConfig.save();
                if (v) NerdModeActivator.activate();
            }));
        rows.add(qteTracker());
        return rows;
    }

    private static FeatureRow dropNotify(OverlayHost host) {
        SoundConfig defaults = new SoundConfig("minecraft:entity.ender_dragon.growl", 0.85f, 1.00f);
        Widget drawer = SoundSection.of(host, () -> DropNotifier.soundConfig,
            v -> DropNotifier.soundConfig = v, defaults);
        return new FeatureRow("Drop Notify", "Announce 8x / 16x drops.")
            .killedKey("drop_notify")
            .toggle(() -> DropNotifier.dropNotifyEnabled,
                v -> { DropNotifier.dropNotifyEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static FeatureRow nodeDepletedPing(OverlayHost host) {
        SoundConfig defaults = new SoundConfig("minecraft:block.note_block.bass", 0.85f, 0.60f);
        Widget drawer = SoundSection.of(host, () -> NodeAlertManager.depletionSoundConfig,
            v -> NodeAlertManager.depletionSoundConfig = v, defaults);
        return new FeatureRow("Node Depleted Ping", "Alert when current node depletes.")
            .killedKey("node_depleted_ping")
            .toggle(() -> NodeAlertManager.depletionPingEnabled,
                v -> { NodeAlertManager.depletionPingEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static FeatureRow regenMode(OverlayHost host) {
        SoundConfig defaults = new SoundConfig("minecraft:block.note_block.bell", 1.00f, 1.00f);
        Widget drawer = new Flow.Column(Metrics.DRAWER_GAP)
            .add(tileGrid(
                new CheckTile("Short Ping",
                    () -> NodeAlertManager.regenPingMode == NodeAlertManager.RegenPingMode.SHORT_PING,
                    () -> {
                        NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.SHORT_PING;
                        NodeAlertManager.regenReminderActive = false;
                        IslesPlusConfig.save();
                    }),
                new CheckTile("Ping-Until-Interact",
                    () -> NodeAlertManager.regenPingMode == NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT,
                    () -> {
                        NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT;
                        IslesPlusConfig.save();
                    })))
            .add(SoundSection.of(host, () -> NodeAlertManager.regenSoundConfig,
                v -> NodeAlertManager.regenSoundConfig = v, defaults));
        return new FeatureRow("Regen Mode", "Ping when node regenerates.")
            .killedKey("regen_mode")
            .toggle(() -> NodeAlertManager.regenPingMode != NodeAlertManager.RegenPingMode.OFF,
                enabled -> {
                    if (!enabled) {
                        NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.OFF;
                        NodeAlertManager.regenReminderActive = false;
                    } else if (NodeAlertManager.regenPingMode == NodeAlertManager.RegenPingMode.OFF) {
                        NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT;
                    }
                    IslesPlusConfig.save();
                })
            .drawer(drawer);
    }

    private static FeatureRow qteTracker() {
        Widget drawer = new Flow.Column(Metrics.DRAWER_GAP)
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(new Label("TRACKED EVENTS", Theme.TEXT_LABEL, Fonts.BODY))
                .add(new InfoChip(() -> RowsText.countChip(trackedQteEvents(), 5))))
            .add(tileGrid(
                tile("Luck", () -> QteTracker.qteLuckEnabled,
                    () -> QteTracker.qteLuckEnabled = !QteTracker.qteLuckEnabled)
                    .disabledWhen(() -> FeatureFlags.isKilled("qte_tracker_luck")),
                tile("Exp", () -> QteTracker.qteExpEnabled,
                    () -> QteTracker.qteExpEnabled = !QteTracker.qteExpEnabled)
                    .disabledWhen(() -> FeatureFlags.isKilled("qte_tracker_exp")),
                tile("Chance", () -> QteTracker.qteChanceEnabled,
                    () -> QteTracker.qteChanceEnabled = !QteTracker.qteChanceEnabled)
                    .disabledWhen(() -> FeatureFlags.isKilled("qte_tracker_chance")),
                tile("Coins", () -> QteTracker.qteCoinsEnabled,
                    () -> QteTracker.qteCoinsEnabled = !QteTracker.qteCoinsEnabled)
                    .disabledWhen(() -> FeatureFlags.isKilled("qte_tracker_coins")),
                tile("Tick Skip", () -> QteTracker.qteTickSkipEnabled,
                    () -> QteTracker.qteTickSkipEnabled = !QteTracker.qteTickSkipEnabled)
                    .beta()
                    .disabledWhen(() -> FeatureFlags.isKilled("qte_tracker_tickskip"))));
        return new FeatureRow("QTE Tracker", "Line + box on quick time events.")
            .note("Shaders may affect line rendering")
            .killedKey("qte_tracker")
            .toggle(() -> QteTracker.qteTrackerEnabled,
                v -> { QteTracker.qteTrackerEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static int trackedQteEvents() {
        return count(() -> QteTracker.qteLuckEnabled, () -> QteTracker.qteExpEnabled,
            () -> QteTracker.qteChanceEnabled, () -> QteTracker.qteCoinsEnabled,
            () -> QteTracker.qteTickSkipEnabled);
    }

    // ==============================
    // Rift
    // ==============================

    static List<FeatureRow> rift(OverlayHost host) {
        List<FeatureRow> rows = new ArrayList<>();
        // Player Highlight and the Score Calculator are not spoilers (players are players, the score
        // is on the scoreboard anyway), so no warning popup; the other finders go through riftToggle.
        rows.add(finderRow("Player Highlight", "Highlight nearby players.", "player_finder", false,
            () -> PlayerFinder.playerFinderEnabled,
            v -> { PlayerFinder.playerFinderEnabled = v; if (!v) PlayerFinder.reset(); IslesPlusConfig.save(); },
            () -> PlayerFinder.glowHue, v -> PlayerFinder.glowHue = v,
            () -> PlayerFinder.glowSaturation, v -> PlayerFinder.glowSaturation = v,
            () -> PlayerFinder.glowLightness, v -> PlayerFinder.glowLightness = v));
        rows.add(finderRow("Button Finder", "Highlight buttons.", "button_finder", true,
            () -> SecretFinder.secretFinderEnabled,
            v -> { SecretFinder.secretFinderEnabled = v; if (!v) SecretFinder.reset(); IslesPlusConfig.save(); },
            () -> SecretFinder.glowHue, v -> SecretFinder.glowHue = v,
            () -> SecretFinder.glowSaturation, v -> SecretFinder.glowSaturation = v,
            () -> SecretFinder.glowLightness, v -> SecretFinder.glowLightness = v)
            .note("Shaders may break this feature"));
        rows.add(finderRow("Chest Finder", "Highlight chests.", "chest_finder", true,
            () -> ChestFinder.chestFinderEnabled,
            v -> { ChestFinder.chestFinderEnabled = v; if (!v) ChestFinder.reset(); IslesPlusConfig.save(); },
            () -> ChestFinder.glowHue, v -> ChestFinder.glowHue = v,
            () -> ChestFinder.glowSaturation, v -> ChestFinder.glowSaturation = v,
            () -> ChestFinder.glowLightness, v -> ChestFinder.glowLightness = v));
        rows.add(finderRow("Vending Machine Finder", "Highlight vending machines.", "vending_machine_finder", true,
            () -> VendingMachineFinder.vendingMachineFinderEnabled,
            v -> { VendingMachineFinder.vendingMachineFinderEnabled = v; if (!v) VendingMachineFinder.reset(); IslesPlusConfig.save(); },
            () -> VendingMachineFinder.glowHue, v -> VendingMachineFinder.glowHue = v,
            () -> VendingMachineFinder.glowSaturation, v -> VendingMachineFinder.glowSaturation = v,
            () -> VendingMachineFinder.glowLightness, v -> VendingMachineFinder.glowLightness = v));
        rows.add(finderRow("Mob Finder", "Highlight nearby mobs.", "mob_finder", true,
            () -> MobFinder.mobFinderEnabled,
            v -> { MobFinder.mobFinderEnabled = v; if (!v) MobFinder.reset(); IslesPlusConfig.save(); },
            () -> MobFinder.glowHue, v -> MobFinder.glowHue = v,
            () -> MobFinder.glowSaturation, v -> MobFinder.glowSaturation = v,
            () -> MobFinder.glowLightness, v -> MobFinder.glowLightness = v));
        rows.add(scoreCalculator());   // last of six = the bottom-right slot
        return rows;
    }

    /** A finder row with a colour drawer bound to the finder's hue/saturation/lightness fields.
     * {@code riftWarning}: wrap the toggle in {@link #riftToggle} (first-enable spoiler popup). */
    private static FeatureRow finderRow(String title, String description, String killKey, boolean riftWarning,
            BooleanSupplier enabled, Consumer<Boolean> apply,
            Supplier<Float> hue, Consumer<Float> setHue,
            Supplier<Float> sat, Consumer<Float> setSat,
            Supplier<Float> light, Consumer<Float> setLight) {
        Widget drawer = GlowColorDrawer.of(hue, setHue, sat, setSat, light, setLight);
        return new FeatureRow(title, description)
            .killedKey(killKey)
            .toggle(enabled, riftWarning ? riftToggle(apply) : apply)
            .drawer(drawer);
    }

    private static FeatureRow scoreCalculator() {
        Widget drawer = new Flow.Column(Metrics.DRAWER_GAP)
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(new Label("HUD OPTIONS", Theme.TEXT_LABEL, Fonts.BODY))
                .add(new InfoChip(() -> RowsText.countChip(hudOptions(), 2))))
            .add(tileGrid(
                tile("Show player count", () -> RankCalculator.showPlayerCount,
                    () -> RankCalculator.showPlayerCount = !RankCalculator.showPlayerCount),
                tile("Show rank drop timer", () -> RankCalculator.showRankDropTimer,
                    () -> RankCalculator.showRankDropTimer = !RankCalculator.showRankDropTimer)))
            .add(new FootnoteBox("Mob = 1 point", "Chest = 5 points", "Boss = 25 points"));
        return new FeatureRow("Score Calculator", "Live rank HUD from Rift scoreboard.")
            .killedKey("rank_calculator")
            .toggle(() -> RankCalculator.rankCalculatorEnabled,
                v -> { RankCalculator.rankCalculatorEnabled = v; IslesPlusConfig.save(); })
            .drawer(drawer);
    }

    private static int hudOptions() {
        return count(() -> RankCalculator.showPlayerCount, () -> RankCalculator.showRankDropTimer);
    }

    /**
     * Wraps a rift toggle callback to show a first-time warning before enabling.
     * Disabling always works without the warning. (Copied from the old dashboard's card registration logic.)
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

    // ==============================
    // Helpers
    // ==============================

    /** Two-column tile grid (one column below 150 px wide), gap 4. */
    static Widget tileGrid(Widget... tiles) {
        // Two per line only while every label fits; otherwise one tile per line, never an ellipsis.
        Flow.Grid grid = new Flow.Grid(2, ROW_GAP, TILE_TWO_COL_MIN_WIDTH).minCellWidth(() -> {
            int widest = 0;
            for (Widget t : tiles) if (t instanceof CheckTile tile) widest = Math.max(widest, tile.naturalWidth());
            return widest;
        });
        for (Widget t : tiles) grid.add(t);
        return grid;
    }

    /** A drawer tile whose click runs {@code toggle} and then persists the config. */
    private static CheckTile tile(String label, BooleanSupplier get, Runnable toggle) {
        return new CheckTile(label, get, () -> { toggle.run(); IslesPlusConfig.save(); });
    }

    /** How many of the given flags are currently true. */
    private static int count(BooleanSupplier... flags) {
        int n = 0;
        for (BooleanSupplier f : flags) if (f.getAsBoolean()) n++;
        return n;
    }
}
