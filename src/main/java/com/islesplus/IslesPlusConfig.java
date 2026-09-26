package com.islesplus;

import com.islesplus.features.bosstracker.BossTimerHud;
import com.islesplus.features.harvestables.HarvestableHighlighter;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudLayout;
import com.islesplus.hud.HudPlacement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islesplus.features.chatfilter.ChatFilter;
import com.islesplus.sync.Announcements;
import com.islesplus.sound.SoundConfig;

import com.islesplus.features.bosstracker.BossTracker;
import com.islesplus.features.autoparty.AutoParty;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier;
import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.berryalert.BerryAlert;
import com.islesplus.features.foodbuff.FoodBuffTimer;
import com.islesplus.features.voidrift.VoidRiftTimer;
import com.islesplus.features.eggtimer.EggTimer;
import com.islesplus.features.storagecount.StorageCount;
import com.islesplus.features.treasurechest.TreasureChestFinder;
import com.islesplus.features.dropnotifier.DropNotifier;
import com.islesplus.features.inventorynotifier.InventoryNotifier;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.nodealertmanager.NodeAlertManager;
import com.islesplus.features.noderadius.NodeRadiusRenderer;
import com.islesplus.features.harvesttimer.HarvestTimer;
import com.islesplus.features.rollpercent.ItemAge;
import com.islesplus.features.rollpercent.RollPercent;
import com.islesplus.features.plushiefinder.PlushieFinder;
import com.islesplus.features.playerfinder.PlayerFinder;
import com.islesplus.features.rankcalculator.RankCalculator;
import com.islesplus.features.qtetracker.QteTracker;
import com.islesplus.features.quickactions.QuickActions;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.slotlocker.SlotLocker;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.features.waystonefinder.WaystoneFinder;
import com.islesplus.logging.IslesLog;
import com.islesplus.sound.SoundController;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class IslesPlusConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR  =
        FabricLoader.getInstance().getConfigDir().resolve("islesplus");
    private static final Path CONFIG_PATH =
        CONFIG_DIR.resolve("islesplus.json");
    /** old location from before we had a folder, gets migrated on first load */
    private static final Path LEGACY_MAIN_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("islesplus.json");
    private static final Path LEGACY_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("islesplus-slot-locks.json");

    public static int maxMatches = 500;

    private IslesPlusConfig() {}

    public static void load() {
        JsonObject obj = readJson(CONFIG_PATH);
        if (obj == null) {
            obj = tryMigrateMain();
        }
        if (obj == null) {
            obj = tryMigrateLegacy();
        }
        if (obj == null) return;

        IslesClient.chatUpdatesEnabled                    = getBool(obj,  "chatUpdatesEnabled",              false);
        Announcements.lastShownId                         = getString(obj, "lastAnnouncementId",             "");
        Announcements.lastShownIssued                     = getString(obj, "lastAnnouncementIssued",         "");
        Announcements.restoreFeed();
        IslesClient.modOnlySoundsEnabled                  = getBool(obj,  "modOnlySoundsEnabled",            false);
        InventoryNotifier.inventoryFullNotifyEnabled = getBool(obj,  "inventoryFullNotifyEnabled", false);
        InventoryNotifier.soundConfig = loadSoundConfig(obj, "inventoryFull",
            InventoryNotifier.soundConfig);
        DropNotifier.dropNotifyEnabled = getBool(obj, "dropNotifyEnabled", true);
        DropNotifier.soundConfig = loadSoundConfig(obj, "dropNotify",
            DropNotifier.soundConfig);
        TreasureChestFinder.treasureChestsEnabled = getBool(obj, "treasureChestsEnabled", true);
        FoodBuffTimer.foodBuffTimerEnabled = getBool(obj, "foodBuffTimerEnabled", true);
        VoidRiftTimer.mode = VoidRiftTimer.Mode.ALERT;
        if (obj.has("voidRiftMode")) {
            try { VoidRiftTimer.mode = VoidRiftTimer.Mode.valueOf(obj.get("voidRiftMode").getAsString()); }
            catch (RuntimeException ignored) {}
        }
        VoidRiftTimer.soundConfig = loadSoundConfig(obj, "voidRift", VoidRiftTimer.soundConfig);
        EggTimer.eggTimerEnabled = getBool(obj, "eggTimerEnabled", true);
        StorageCount.storageCountEnabled = getBool(obj, "storageCountEnabled", true);
        EggTimer.soundConfig = loadSoundConfig(obj, "eggTimer", EggTimer.soundConfig);
        BerryAlert.berryAlertEnabled = getBool(obj, "berryAlertEnabled", true);
        BerryAlert.soundConfig = loadSoundConfig(obj, "berryAlert",
            BerryAlert.soundConfig);
        NodeAlertManager.depletionPingEnabled = getBool(obj, "depletionPingEnabled", false);
        NodeAlertManager.depletionSoundConfig = loadSoundConfig(obj, "depletionPing",
            NodeAlertManager.depletionSoundConfig);
        NodeAlertManager.regenSoundConfig = loadSoundConfig(obj, "regenPing",
            NodeAlertManager.regenSoundConfig);
        NodeRadiusRenderer.nodeRadiusEnabled        = getBool(obj, "nodeRadiusEnabled",               false);
        PlushieFinder.plushieFinderEnabled          = getBool(obj, "plushieFinderEnabled",            false);
        PlushieFinder.maxDistance                   = getInt(obj,  "plushieMaxDistance",              0);
        PlushieFinder.hideFirstPlushie              = getBool(obj, "plushieHideFirst",                false);
        SlotLocker.slotLockEnabled                  = getBool(obj, "slotLockEnabled",                 true);
        HarvestableHighlighter.enabled              = getBool(obj,  "harvestableHighlighterEnabled",   false);
        HarvestableHighlighter.waypoints            = getBool(obj,  "harvestableWaypoints",            true);
        JsonObject harvestColors = obj.has("harvestableColors") && obj.get("harvestableColors").isJsonObject()
            ? obj.getAsJsonObject("harvestableColors") : new JsonObject();
        for (HarvestableHighlighter.Harvestable h : HarvestableHighlighter.Harvestable.values()) {
            JsonObject c = harvestColors.has(h.name()) && harvestColors.get(h.name()).isJsonObject()
                ? harvestColors.getAsJsonObject(h.name()) : new JsonObject();
            h.hue        = getFloat(c, "hue",        h.defaultHue);
            h.saturation = getFloat(c, "saturation", 1.0f);
            h.lightness  = getFloat(c, "lightness",  0.5f);
        }
        HarvestableHighlighter.hidden.clear();
        if (obj.has("harvestableHidden") && obj.get("harvestableHidden").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("harvestableHidden")) {
                try { HarvestableHighlighter.hidden.add(HarvestableHighlighter.Harvestable.valueOf(el.getAsString())); }
                catch (RuntimeException ignored) {}
            }
        }
        ChestFinder.chestFinderEnabled              = getBool(obj,  "chestFinderEnabled",              false);
        ChestFinder.glowHue                         = getFloat(obj, "chestFinderGlowHue",             0.128f);
        ChestFinder.glowSaturation                  = getFloat(obj, "chestFinderGlowSaturation",      1.0f);
        ChestFinder.glowLightness                   = getFloat(obj, "chestFinderGlowLightness",       0.5f);
        MobFinder.mobFinderEnabled                  = getBool(obj,  "mobFinderEnabled",                false);
        MobFinder.glowHue                           = getFloat(obj, "mobFinderGlowHue",               0.617f);
        MobFinder.glowSaturation                    = getFloat(obj, "mobFinderGlowSaturation",        1.0f);
        MobFinder.glowLightness                     = getFloat(obj, "mobFinderGlowLightness",         0.5f);
        PlayerFinder.playerFinderEnabled            = getBool(obj,  "playerFinderEnabled",             false);
        PlayerFinder.glowHue                        = getFloat(obj, "playerFinderGlowHue",            0.333f);
        PlayerFinder.glowSaturation                 = getFloat(obj, "playerFinderGlowSaturation",     1.0f);
        PlayerFinder.glowLightness                  = getFloat(obj, "playerFinderGlowLightness",      0.5f);
        SecretFinder.secretFinderEnabled            = getBool(obj,  "secretFinderEnabled",             false);
        SecretFinder.glowHue                        = getFloat(obj, "secretFinderGlowHue",            0.917f);
        SecretFinder.glowSaturation                 = getFloat(obj, "secretFinderGlowSaturation",     1.0f);
        SecretFinder.glowLightness                  = getFloat(obj, "secretFinderGlowLightness",      0.5f);
        VendingMachineFinder.vendingMachineFinderEnabled = getBool(obj, "vendingMachineFinderEnabled", false);
        VendingMachineFinder.glowHue                = getFloat(obj, "vendingMachineFinderGlowHue",    0.092f);
        VendingMachineFinder.glowSaturation         = getFloat(obj, "vendingMachineFinderGlowSaturation", 1.0f);
        VendingMachineFinder.glowLightness          = getFloat(obj, "vendingMachineFinderGlowLightness",  0.5f);
        WaystoneFinder.waystoneFinderEnabled        = getBool(obj,  "waystoneFinderEnabled",          true);
        WaystoneFinder.glowHue                      = getFloat(obj, "waystoneFinderGlowHue",          0.13f);
        WaystoneFinder.glowSaturation               = getFloat(obj, "waystoneFinderGlowSaturation",   1.0f);
        WaystoneFinder.glowLightness                = getFloat(obj, "waystoneFinderGlowLightness",    0.5f);
        RankCalculator.rankCalculatorEnabled        = getBool(obj, "rankCalculatorEnabled",           true);
        RankCalculator.showPlayerCount              = getBool(obj, "rankShowPlayerCount",              false);
        RankCalculator.showRankDropTimer            = getBool(obj, "rankShowDropTimer",                false);
        HarvestTimer.harvestTimerEnabled            = getBool(obj, "harvestTimerEnabled",             false);
        RollPercent.rollPercentEnabled              = getBool(obj, "rollPercentEnabled",              true);
        ItemAge.itemAgeEnabled                      = getBool(obj, "itemAgeEnabled",                  true);
        QteTracker.qteTrackerEnabled  = getBool(obj,  "qteTrackerEnabled",   false);
        GroundItemsNotifier.groundItemsNotifierEnabled = getBool(obj, "groundItemsNotifierEnabled", false);
        BossTracker.bossTrackerEnabled = getBool(obj, "bossTrackerEnabled", false);
        BossTracker.autoOpenBossary    = getBool(obj, "bossAutoOpen", false);
        HudLayout.load(obj.has("hudLayout") && obj.get("hudLayout").isJsonObject() ? obj.getAsJsonObject("hudLayout") : null);
        // Before the HUD editor these two only had fixed corners: keep the corner that was picked.
        HudPlacement oldBoss = switch (getString(obj, "bossHudPosition", "")) {
            case "TOP_RIGHT"    -> new HudPlacement(HudAnchor.END,   HudAnchor.START, 10, 10);
            case "BOTTOM_LEFT"  -> new HudPlacement(HudAnchor.START, HudAnchor.END,   10, 10);
            case "BOTTOM_RIGHT" -> new HudPlacement(HudAnchor.END,   HudAnchor.END,   10, 10);
            default -> null;
        };
        if (oldBoss != null) HudLayout.migrate(BossTimerHud.ELEMENT, oldBoss);
        HudPlacement oldBar = switch (getString(obj, "searchBarPosition", "")) {
            case "TOP_CENTER"    -> new HudPlacement(HudAnchor.CENTER, HudAnchor.START, 0, 4);
            case "TOP_RIGHT"     -> new HudPlacement(HudAnchor.END,    HudAnchor.START, 4, 4);
            case "BOTTOM_LEFT"   -> new HudPlacement(HudAnchor.START,  HudAnchor.END,   4, 4);
            case "BOTTOM_CENTER" -> new HudPlacement(HudAnchor.CENTER, HudAnchor.END,   0, 4);
            case "BOTTOM_RIGHT"  -> new HudPlacement(HudAnchor.END,    HudAnchor.END,   4, 4);
            default -> null;
        };
        if (oldBar != null) HudLayout.migrate(InventorySearch.ELEMENT, oldBar);
        BossTracker.hiddenBossNames.clear();
        if (obj.has("bossHiddenBosses") && obj.get("bossHiddenBosses").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("bossHiddenBosses")) {
                if (el.isJsonPrimitive()) BossTracker.hiddenBossNames.add(el.getAsString());
            }
        }
        AutoParty.enabled          = getBool(obj, "autoPartyEnabled",      false);
        AutoParty.autoLeaveDisband = getBool(obj, "autoLeaveDisband",      false);
        AutoParty.friends.clear();
        if (obj.has("autoPartyFriends") && obj.get("autoPartyFriends").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("autoPartyFriends")) {
                if (el.isJsonPrimitive()) AutoParty.friends.add(el.getAsString());
            }
        }
        AutoParty.groups.clear();
        if (obj.has("autoPartyGroups") && obj.get("autoPartyGroups").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("autoPartyGroups")) {
                if (!el.isJsonObject()) continue;
                JsonObject g = el.getAsJsonObject();
                String gName = getString(g, "name", "Group");
                AutoParty.PartyGroup group = new AutoParty.PartyGroup(gName);
                if (g.has("members") && g.get("members").isJsonArray()) {
                    for (JsonElement m : g.getAsJsonArray("members")) {
                        if (m.isJsonPrimitive()) group.members.add(m.getAsString());
                    }
                }
                AutoParty.groups.add(group);
            }
        }
        AutoParty.activeGroupIdx = getInt(obj, "autoPartyActiveGroup", AutoParty.groups.isEmpty() ? -1 : 0);
        GroundItemsNotifier.watchedItems.clear();
        if (obj.has("groundWatchedItems") && obj.get("groundWatchedItems").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("groundWatchedItems")) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                GroundItemsNotifier.WatchedItem item = new GroundItemsNotifier.WatchedItem();
                item.enabled         = getBool(o,  "enabled",       true);
                item.customKeyword   = getString(o, "customKeyword", "");
                item.lineTracker     = getBool(o,  "lineTracker",    true);
                item.screenNotifier  = getBool(o,  "screenNotifier", true);
                item.highlight       = getBool(o,  "highlight",      true);
                item.soundPing       = getBool(o,  "soundPing",      false);
                item.soundConfig     = loadSoundConfig(o, "itemSound", item.soundConfig);
                GroundItemsNotifier.watchedItems.add(item);
            }
        }

        QteTracker.qteLuckEnabled      = getBool(obj,  "qteLuckEnabled",      false);
        QteTracker.qteExpEnabled       = getBool(obj,  "qteExpEnabled",       false);
        QteTracker.qteChanceEnabled    = getBool(obj,  "qteChanceEnabled",    false);
        QteTracker.qteCoinsEnabled     = getBool(obj,  "qteCoinsEnabled",     false);
        QteTracker.qteTickSkipEnabled  = getBool(obj,  "qteTickSkipEnabled",  false);
        ChatFilter.chatFilterEnabled                = getBool(obj,  "chatFilterEnabled",               false);
        ChatFilter.filterManaMeteor                = getBool(obj,  "filterManaMeteor",                false);
        ChatFilter.filterGuildChat                 = getBool(obj,  "filterGuildChat",                  false);
        ChatFilter.filterDeaths                    = getBool(obj,  "filterDeaths",                     true);
        InventorySearch.inventorySearchEnabled      = getBool(obj,  "inventorySearchEnabled",           true);
        maxMatches                                  = getInt(obj,   "maxMatches",                       500);

        NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.OFF;
        if (obj.has("regenPingMode")) {
            try {
                NodeAlertManager.regenPingMode =
                    NodeAlertManager.RegenPingMode.valueOf(obj.get("regenPingMode").getAsString());
            } catch (RuntimeException ignored) {}
        }

        QuickActions.enabled = getBool(obj, "quickActionsEnabled", true);
        QuickActions.showBackground = getBool(obj, "quickActionsBackground", true);
        QuickActions.loadJson(obj.has("quickActions") && obj.get("quickActions").isJsonArray()
            ? obj.getAsJsonArray("quickActions") : null);
        if (obj.has("lockedSlots") && obj.get("lockedSlots").isJsonArray()) {
            SlotLocker.setLockedSlots(obj.getAsJsonArray("lockedSlots"));
        }

        if (IslesClient.modOnlySoundsEnabled) {
            SoundController.setModOnlySoundsEnabled(true);
        }
    }

    /** Writes to a sibling temp file and moves it into place, so a crash or kill mid-write can
     * never leave a half-written config (which would lose party groups, watched items, ...). */
    private static void writeAtomically(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        try {
            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // No atomic move here, or something (antivirus, a sync client) holds the file open in a
            // way that refuses the swap: a plain write still gets the settings saved.
            Files.writeString(target, content);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public static void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("chatUpdatesEnabled",               IslesClient.chatUpdatesEnabled);
        obj.addProperty("lastAnnouncementId",               Announcements.lastShownId);
        obj.addProperty("lastAnnouncementIssued",           Announcements.lastShownIssued);
        obj.addProperty("modOnlySoundsEnabled",             IslesClient.modOnlySoundsEnabled);
        obj.addProperty("inventoryFullNotifyEnabled", InventoryNotifier.inventoryFullNotifyEnabled);
        saveSoundConfig(obj, "inventoryFull", InventoryNotifier.soundConfig);
        obj.addProperty("dropNotifyEnabled", DropNotifier.dropNotifyEnabled);
        saveSoundConfig(obj, "dropNotify", DropNotifier.soundConfig);
        obj.addProperty("treasureChestsEnabled", TreasureChestFinder.treasureChestsEnabled);
        obj.addProperty("foodBuffTimerEnabled", FoodBuffTimer.foodBuffTimerEnabled);
        obj.addProperty("voidRiftMode", VoidRiftTimer.mode.name());
        saveSoundConfig(obj, "voidRift", VoidRiftTimer.soundConfig);
        obj.addProperty("eggTimerEnabled", EggTimer.eggTimerEnabled);
        obj.addProperty("storageCountEnabled", StorageCount.storageCountEnabled);
        saveSoundConfig(obj, "eggTimer", EggTimer.soundConfig);
        obj.addProperty("berryAlertEnabled", BerryAlert.berryAlertEnabled);
        saveSoundConfig(obj, "berryAlert", BerryAlert.soundConfig);
        obj.addProperty("depletionPingEnabled", NodeAlertManager.depletionPingEnabled);
        saveSoundConfig(obj, "depletionPing", NodeAlertManager.depletionSoundConfig);
        saveSoundConfig(obj, "regenPing", NodeAlertManager.regenSoundConfig);
        obj.addProperty("nodeRadiusEnabled",             NodeRadiusRenderer.nodeRadiusEnabled);
        obj.addProperty("regenPingMode",                 NodeAlertManager.regenPingMode.name());
        obj.addProperty("plushieFinderEnabled",          PlushieFinder.plushieFinderEnabled);
        obj.addProperty("plushieMaxDistance",            PlushieFinder.maxDistance);
        obj.addProperty("plushieHideFirst",              PlushieFinder.hideFirstPlushie);
        obj.addProperty("slotLockEnabled",               SlotLocker.slotLockEnabled);
        obj.addProperty("harvestableHighlighterEnabled", HarvestableHighlighter.enabled);
        obj.addProperty("harvestableWaypoints",          HarvestableHighlighter.waypoints);
        JsonObject harvestColorsOut = new JsonObject();
        for (HarvestableHighlighter.Harvestable h : HarvestableHighlighter.Harvestable.values()) {
            JsonObject c = new JsonObject();
            c.addProperty("hue", h.hue);
            c.addProperty("saturation", h.saturation);
            c.addProperty("lightness", h.lightness);
            harvestColorsOut.add(h.name(), c);
        }
        obj.add("harvestableColors",                     harvestColorsOut);
        JsonArray harvestHiddenArr = new JsonArray();
        for (HarvestableHighlighter.Harvestable h : HarvestableHighlighter.hidden) harvestHiddenArr.add(h.name());
        obj.add("harvestableHidden",                     harvestHiddenArr);
        obj.addProperty("chestFinderEnabled",            ChestFinder.chestFinderEnabled);
        obj.addProperty("chestFinderGlowHue",            ChestFinder.glowHue);
        obj.addProperty("chestFinderGlowSaturation",     ChestFinder.glowSaturation);
        obj.addProperty("chestFinderGlowLightness",      ChestFinder.glowLightness);
        obj.addProperty("mobFinderEnabled",              MobFinder.mobFinderEnabled);
        obj.addProperty("mobFinderGlowHue",              MobFinder.glowHue);
        obj.addProperty("mobFinderGlowSaturation",       MobFinder.glowSaturation);
        obj.addProperty("mobFinderGlowLightness",        MobFinder.glowLightness);
        obj.addProperty("playerFinderEnabled",           PlayerFinder.playerFinderEnabled);
        obj.addProperty("playerFinderGlowHue",           PlayerFinder.glowHue);
        obj.addProperty("playerFinderGlowSaturation",    PlayerFinder.glowSaturation);
        obj.addProperty("playerFinderGlowLightness",     PlayerFinder.glowLightness);
        obj.addProperty("secretFinderEnabled",           SecretFinder.secretFinderEnabled);
        obj.addProperty("secretFinderGlowHue",           SecretFinder.glowHue);
        obj.addProperty("secretFinderGlowSaturation",    SecretFinder.glowSaturation);
        obj.addProperty("secretFinderGlowLightness",     SecretFinder.glowLightness);
        obj.addProperty("vendingMachineFinderEnabled",   VendingMachineFinder.vendingMachineFinderEnabled);
        obj.addProperty("vendingMachineFinderGlowHue",   VendingMachineFinder.glowHue);
        obj.addProperty("vendingMachineFinderGlowSaturation", VendingMachineFinder.glowSaturation);
        obj.addProperty("vendingMachineFinderGlowLightness",  VendingMachineFinder.glowLightness);
        obj.addProperty("waystoneFinderEnabled",         WaystoneFinder.waystoneFinderEnabled);
        obj.addProperty("waystoneFinderGlowHue",         WaystoneFinder.glowHue);
        obj.addProperty("waystoneFinderGlowSaturation",  WaystoneFinder.glowSaturation);
        obj.addProperty("waystoneFinderGlowLightness",   WaystoneFinder.glowLightness);
        obj.addProperty("rankCalculatorEnabled",         RankCalculator.rankCalculatorEnabled);
        obj.addProperty("rankShowPlayerCount",           RankCalculator.showPlayerCount);
        obj.addProperty("rankShowDropTimer",             RankCalculator.showRankDropTimer);
        obj.addProperty("harvestTimerEnabled",           HarvestTimer.harvestTimerEnabled);
        obj.addProperty("rollPercentEnabled",            RollPercent.rollPercentEnabled);
        obj.addProperty("itemAgeEnabled",                ItemAge.itemAgeEnabled);
        obj.addProperty("qteTrackerEnabled",    QteTracker.qteTrackerEnabled);
        obj.addProperty("groundItemsNotifierEnabled", GroundItemsNotifier.groundItemsNotifierEnabled);
        obj.addProperty("bossTrackerEnabled", BossTracker.bossTrackerEnabled);
        obj.addProperty("bossAutoOpen",        BossTracker.autoOpenBossary);
        obj.add("hudLayout",                 HudLayout.toJson());
        JsonArray hiddenArr = new JsonArray();
        for (String name : BossTracker.hiddenBossNames) hiddenArr.add(name);
        obj.add("bossHiddenBosses", hiddenArr);
        obj.addProperty("autoPartyEnabled",  AutoParty.enabled);
        obj.addProperty("autoLeaveDisband",  AutoParty.autoLeaveDisband);
        JsonArray partyFriendsArr = new JsonArray();
        for (String f : AutoParty.friends) partyFriendsArr.add(f);
        obj.add("autoPartyFriends", partyFriendsArr);
        JsonArray groupsArr = new JsonArray();
        for (AutoParty.PartyGroup group : AutoParty.groups) {
            JsonObject g = new JsonObject();
            g.addProperty("name", group.name);
            JsonArray membersArr = new JsonArray();
            for (String m : group.members) membersArr.add(m);
            g.add("members", membersArr);
            groupsArr.add(g);
        }
        obj.add("autoPartyGroups", groupsArr);
        obj.addProperty("autoPartyActiveGroup", AutoParty.activeGroupIdx);
        JsonArray watchedArr = new JsonArray();
        for (GroundItemsNotifier.WatchedItem item : GroundItemsNotifier.watchedItems) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled",        item.enabled);
            o.addProperty("customKeyword",  item.customKeyword);
            o.addProperty("lineTracker",    item.lineTracker);
            o.addProperty("screenNotifier", item.screenNotifier);
            o.addProperty("highlight",      item.highlight);
            o.addProperty("soundPing",      item.soundPing);
            saveSoundConfig(o, "itemSound", item.soundConfig);
            watchedArr.add(o);
        }
        obj.add("groundWatchedItems", watchedArr);

        obj.addProperty("qteLuckEnabled",        QteTracker.qteLuckEnabled);
        obj.addProperty("qteExpEnabled",         QteTracker.qteExpEnabled);
        obj.addProperty("qteChanceEnabled",      QteTracker.qteChanceEnabled);
        obj.addProperty("qteCoinsEnabled",       QteTracker.qteCoinsEnabled);
        obj.addProperty("qteTickSkipEnabled",    QteTracker.qteTickSkipEnabled);
        obj.addProperty("chatFilterEnabled",              ChatFilter.chatFilterEnabled);
        obj.addProperty("filterManaMeteor",              ChatFilter.filterManaMeteor);
        obj.addProperty("filterGuildChat",               ChatFilter.filterGuildChat);
        obj.addProperty("filterDeaths",                  ChatFilter.filterDeaths);
        obj.addProperty("inventorySearchEnabled",        InventorySearch.inventorySearchEnabled);
        obj.addProperty("maxMatches",                    maxMatches);
        obj.add("lockedSlots",                           SlotLocker.getLockedSlotsJson());
        obj.addProperty("quickActionsEnabled",           QuickActions.enabled);
        obj.addProperty("quickActionsBackground",        QuickActions.showBackground);
        obj.add("quickActions",                          QuickActions.toJson());
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            writeAtomically(CONFIG_PATH, GSON.toJson(obj));
        } catch (IOException e) {
            IslesLog.runtimeWarn("[Isles+] Failed to save config", e);
        }
    }

    private static JsonObject readJson(Path path) {
        if (!Files.exists(path)) return null;
        try {
            String text = Files.readString(path);
            JsonElement parsed = JsonParser.parseString(text);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception e) {
            IslesLog.runtimeWarn("[Isles+] Failed to read config at " + path + ", starting fresh", e);
            return null;
        }
    }

    private static JsonObject tryMigrateMain() {
        JsonObject obj = readJson(LEGACY_MAIN_PATH);
        if (obj == null) return null;
        IslesLog.runtimeWarn("[Isles+] Migrating config from " + LEGACY_MAIN_PATH + " to " + CONFIG_PATH);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            writeAtomically(CONFIG_PATH, GSON.toJson(obj));
            Files.deleteIfExists(LEGACY_MAIN_PATH); // only runs if write succeeded
        } catch (IOException e) {
            IslesLog.runtimeWarn("[Isles+] Failed to migrate config", e);
        }
        return obj;
    }

    private static JsonObject tryMigrateLegacy() {
        JsonObject legacy = readJson(LEGACY_PATH);
        if (legacy == null) return null;
        JsonObject migrated = new JsonObject();
        if (legacy.has("lockedSlots")) {
            migrated.add("lockedSlots", legacy.get("lockedSlots"));
        }
        return migrated;
    }

    private static boolean getBool(JsonObject obj, String key, boolean defaultValue) {
        if (!obj.has(key)) return defaultValue;
        JsonElement element = obj.get(key);
        return element.isJsonPrimitive() ? element.getAsBoolean() : defaultValue;
    }

    private static float getFloat(JsonObject obj, String key, float defaultValue) {
        if (!obj.has(key)) return defaultValue;
        JsonElement element = obj.get(key);
        return element.isJsonPrimitive() ? element.getAsFloat() : defaultValue;
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        if (!obj.has(key)) return defaultValue;
        JsonElement element = obj.get(key);
        return element.isJsonPrimitive() ? element.getAsInt() : defaultValue;
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        if (!obj.has(key)) return defaultValue;
        JsonElement element = obj.get(key);
        return element.isJsonPrimitive() ? element.getAsString() : defaultValue;
    }

    private static SoundConfig loadSoundConfig(JsonObject obj, String prefix, SoundConfig fallback) {
        String soundId = getString(obj, prefix + "SoundId", fallback.soundId);
        float  volume  = getFloat(obj,  prefix + "Volume",  fallback.volume);
        float  pitch   = getFloat(obj,  prefix + "Pitch",   fallback.pitch);
        return new SoundConfig(soundId, volume, pitch);
    }

    private static void saveSoundConfig(JsonObject obj, String prefix, SoundConfig config) {
        obj.addProperty(prefix + "SoundId", config.soundId);
        obj.addProperty(prefix + "Volume",  config.volume);
        obj.addProperty(prefix + "Pitch",   config.pitch);
    }

}
