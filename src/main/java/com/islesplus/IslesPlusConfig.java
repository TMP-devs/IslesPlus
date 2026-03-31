package com.islesplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islesplus.features.chatfilter.ChatFilter;
import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;

import java.util.ArrayList;
import com.islesplus.features.bosstracker.BossTracker;
import com.islesplus.features.autoparty.AutoParty;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier;
import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.dropnotifier.DropNotifier;
import com.islesplus.features.inventorynotifier.InventoryNotifier;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.nodealertmanager.NodeAlertManager;
import com.islesplus.features.noderadius.NodeRadiusRenderer;
import com.islesplus.features.harvesttimer.HarvestTimer;
import com.islesplus.features.plushiefinder.PlushieFinder;
import com.islesplus.features.playerfinder.PlayerFinder;
import com.islesplus.features.rankcalculator.RankCalculator;
import com.islesplus.features.qtetracker.QteTracker;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.slotlocker.SlotLocker;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
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
    /** Old flat location - migrate on first load if found. */
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
        IslesClient.modOnlySoundsEnabled                  = getBool(obj,  "modOnlySoundsEnabled",            false);
        InventoryNotifier.inventoryFullNotifyEnabled = getBool(obj,  "inventoryFullNotifyEnabled", false);
        InventoryNotifier.soundConfig = loadSoundConfig(obj, "inventoryFull",
            InventoryNotifier.soundConfig);
        DropNotifier.dropNotifyEnabled = getBool(obj, "dropNotifyEnabled", true);
        DropNotifier.soundConfig = loadSoundConfig(obj, "dropNotify",
            DropNotifier.soundConfig);
        NodeAlertManager.depletionPingEnabled = getBool(obj, "depletionPingEnabled", true);
        NodeAlertManager.depletionSoundConfig = loadSoundConfig(obj, "depletionPing",
            NodeAlertManager.depletionSoundConfig);
        NodeAlertManager.regenSoundConfig = loadSoundConfig(obj, "regenPing",
            NodeAlertManager.regenSoundConfig);
        NodeRadiusRenderer.nodeRadiusEnabled        = getBool(obj, "nodeRadiusEnabled",               false);
        PlushieFinder.plushieFinderEnabled          = getBool(obj, "plushieFinderEnabled",            true);
        SlotLocker.slotLockEnabled                  = getBool(obj, "slotLockEnabled",                 true);
        ChestFinder.chestFinderEnabled              = getBool(obj,  "chestFinderEnabled",              false);
        ChestFinder.glowHue                         = getFloat(obj, "chestFinderGlowHue",             0.128f);
        MobFinder.mobFinderEnabled                  = getBool(obj,  "mobFinderEnabled",                false);
        MobFinder.glowHue                           = getFloat(obj, "mobFinderGlowHue",               0.617f);
        PlayerFinder.playerFinderEnabled            = getBool(obj,  "playerFinderEnabled",             false);
        PlayerFinder.glowHue                        = getFloat(obj, "playerFinderGlowHue",            0.333f);
        SecretFinder.secretFinderEnabled            = getBool(obj,  "secretFinderEnabled",             false);
        SecretFinder.glowHue                        = getFloat(obj, "secretFinderGlowHue",            0.917f);
        VendingMachineFinder.vendingMachineFinderEnabled = getBool(obj, "vendingMachineFinderEnabled", false);
        VendingMachineFinder.glowHue                = getFloat(obj, "vendingMachineFinderGlowHue",    0.092f);
        RankCalculator.rankCalculatorEnabled        = getBool(obj, "rankCalculatorEnabled",           false);
        RankCalculator.showPlayerCount              = getBool(obj, "rankShowPlayerCount",              false);
        RankCalculator.showRankDropTimer            = getBool(obj, "rankShowDropTimer",                false);
        HarvestTimer.harvestTimerEnabled            = getBool(obj, "harvestTimerEnabled",             false);
        QteTracker.qteTrackerEnabled  = getBool(obj,  "qteTrackerEnabled",   true);
        GroundItemsNotifier.groundItemsNotifierEnabled = getBool(obj, "groundItemsNotifierEnabled", false);
        BossTracker.bossTrackerEnabled = getBool(obj, "bossTrackerEnabled", false);
        BossTracker.autoOpenBossary    = getBool(obj, "bossAutoOpen", false);
        if (obj.has("bossHudPosition")) {
            try { BossTracker.hudPosition = BossTracker.BossHudPosition.valueOf(obj.get("bossHudPosition").getAsString()); }
            catch (RuntimeException ignored) {}
        }
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

        QteTracker.qteLuckEnabled      = getBool(obj,  "qteLuckEnabled",      true);
        QteTracker.qteExpEnabled       = getBool(obj,  "qteExpEnabled",       false);
        QteTracker.qteChanceEnabled    = getBool(obj,  "qteChanceEnabled",    true);
        QteTracker.qteCoinsEnabled     = getBool(obj,  "qteCoinsEnabled",     false);
        QteTracker.qteTickSkipEnabled  = getBool(obj,  "qteTickSkipEnabled",  false);
        ChatFilter.chatFilterEnabled                = getBool(obj,  "chatFilterEnabled",               false);
        ChatFilter.filterManaMeteor                = getBool(obj,  "filterManaMeteor",                false);
        ChatFilter.filterGuildChat                 = getBool(obj,  "filterGuildChat",                  false);
        InventorySearch.inventorySearchEnabled      = getBool(obj,  "inventorySearchEnabled",           true);
        if (obj.has("searchBarPosition")) {
            try {
                InventorySearch.barPosition =
                    InventorySearch.SearchBarPosition.valueOf(obj.get("searchBarPosition").getAsString());
            } catch (RuntimeException ignored) {}
        }
        maxMatches                                  = getInt(obj,   "maxMatches",                       500);

        NodeAlertManager.regenPingMode = NodeAlertManager.RegenPingMode.PING_UNTIL_INTERACT;
        if (obj.has("regenPingMode")) {
            try {
                NodeAlertManager.regenPingMode =
                    NodeAlertManager.RegenPingMode.valueOf(obj.get("regenPingMode").getAsString());
            } catch (RuntimeException ignored) {}
        }

        if (obj.has("lockedSlots") && obj.get("lockedSlots").isJsonArray()) {
            SlotLocker.setLockedSlots(obj.getAsJsonArray("lockedSlots"));
        }

        if (IslesClient.modOnlySoundsEnabled) {
            SoundController.setModOnlySoundsEnabled(true);
        }
        rebuildModSoundsAllowlist();
    }

    public static void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("chatUpdatesEnabled",               IslesClient.chatUpdatesEnabled);
        obj.addProperty("modOnlySoundsEnabled",             IslesClient.modOnlySoundsEnabled);
        obj.addProperty("inventoryFullNotifyEnabled", InventoryNotifier.inventoryFullNotifyEnabled);
        saveSoundConfig(obj, "inventoryFull", InventoryNotifier.soundConfig);
        obj.addProperty("dropNotifyEnabled", DropNotifier.dropNotifyEnabled);
        saveSoundConfig(obj, "dropNotify", DropNotifier.soundConfig);
        obj.addProperty("depletionPingEnabled", NodeAlertManager.depletionPingEnabled);
        saveSoundConfig(obj, "depletionPing", NodeAlertManager.depletionSoundConfig);
        saveSoundConfig(obj, "regenPing", NodeAlertManager.regenSoundConfig);
        obj.addProperty("nodeRadiusEnabled",             NodeRadiusRenderer.nodeRadiusEnabled);
        obj.addProperty("regenPingMode",                 NodeAlertManager.regenPingMode.name());
        obj.addProperty("plushieFinderEnabled",          PlushieFinder.plushieFinderEnabled);
        obj.addProperty("slotLockEnabled",               SlotLocker.slotLockEnabled);
        obj.addProperty("chestFinderEnabled",            ChestFinder.chestFinderEnabled);
        obj.addProperty("chestFinderGlowHue",            ChestFinder.glowHue);
        obj.addProperty("mobFinderEnabled",              MobFinder.mobFinderEnabled);
        obj.addProperty("mobFinderGlowHue",              MobFinder.glowHue);
        obj.addProperty("playerFinderEnabled",           PlayerFinder.playerFinderEnabled);
        obj.addProperty("playerFinderGlowHue",           PlayerFinder.glowHue);
        obj.addProperty("secretFinderEnabled",           SecretFinder.secretFinderEnabled);
        obj.addProperty("secretFinderGlowHue",           SecretFinder.glowHue);
        obj.addProperty("vendingMachineFinderEnabled",   VendingMachineFinder.vendingMachineFinderEnabled);
        obj.addProperty("vendingMachineFinderGlowHue",   VendingMachineFinder.glowHue);
        obj.addProperty("rankCalculatorEnabled",         RankCalculator.rankCalculatorEnabled);
        obj.addProperty("rankShowPlayerCount",           RankCalculator.showPlayerCount);
        obj.addProperty("rankShowDropTimer",             RankCalculator.showRankDropTimer);
        obj.addProperty("harvestTimerEnabled",           HarvestTimer.harvestTimerEnabled);
        obj.addProperty("qteTrackerEnabled",    QteTracker.qteTrackerEnabled);
        obj.addProperty("groundItemsNotifierEnabled", GroundItemsNotifier.groundItemsNotifierEnabled);
        obj.addProperty("bossTrackerEnabled", BossTracker.bossTrackerEnabled);
        obj.addProperty("bossAutoOpen",        BossTracker.autoOpenBossary);
        obj.addProperty("bossHudPosition",     BossTracker.hudPosition.name());
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
        obj.addProperty("inventorySearchEnabled",        InventorySearch.inventorySearchEnabled);
        obj.addProperty("searchBarPosition",             InventorySearch.barPosition.name());
        obj.addProperty("maxMatches",                    maxMatches);
        obj.add("lockedSlots",                           SlotLocker.getLockedSlotsJson());
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(obj));
        } catch (IOException e) {
            IslesLog.runtimeWarn("[Isles+] Failed to save config", e);
        }
        rebuildModSoundsAllowlist();
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
            Files.writeString(CONFIG_PATH, GSON.toJson(obj));
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

    /**
     * Rebuilds ModSounds' allowlist from the sounds that are actually configured
     * on currently-enabled features. Only these exact sound IDs will pass through
     * the Mod-only Sounds filter.
     */
    private static void rebuildModSoundsAllowlist() {
        ArrayList<String> ids = new ArrayList<>();
        if (InventoryNotifier.inventoryFullNotifyEnabled)
            ids.add(InventoryNotifier.soundConfig.soundId);
        if (DropNotifier.dropNotifyEnabled)
            ids.add(DropNotifier.soundConfig.soundId);
        if (NodeAlertManager.depletionPingEnabled)
            ids.add(NodeAlertManager.depletionSoundConfig.soundId);
        if (NodeAlertManager.regenPingMode != NodeAlertManager.RegenPingMode.OFF)
            ids.add(NodeAlertManager.regenSoundConfig.soundId);
        if (GroundItemsNotifier.groundItemsNotifierEnabled) {
            for (GroundItemsNotifier.WatchedItem item : GroundItemsNotifier.watchedItems) {
                if (item.enabled && item.soundPing)
                    ids.add(item.soundConfig.soundId);
            }
        }
        ModSounds.rebuildActiveSounds(ids);
    }
}
