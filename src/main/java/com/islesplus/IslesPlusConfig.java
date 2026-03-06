package com.islesplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islesplus.features.chatfilter.ChatFilter;
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
        InventoryNotifier.inventoryFullNotifyEnabled       = getBool(obj,  "inventoryFullNotifyEnabled",      false);
        InventoryNotifier.inventoryFullVolume              = getFloat(obj, "inventoryFullVolume",             0.5f);
        DropNotifier.dropNotifyEnabled              = getBool(obj,  "dropNotifyEnabled",               true);
        DropNotifier.dropNotifyVolume               = getFloat(obj, "dropNotifyVolume",                0.5f);
        NodeAlertManager.depletionPingEnabled       = getBool(obj,  "depletionPingEnabled",            true);
        NodeAlertManager.depletionPingVolume        = getFloat(obj, "depletionPingVolume",             0.5f);
        NodeAlertManager.regenPingVolume            = getFloat(obj, "regenPingVolume",                 0.5f);
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
        HarvestTimer.harvestTimerEnabled            = getBool(obj, "harvestTimerEnabled",             false);
        QteTracker.qteTrackerEnabled  = getBool(obj,  "qteTrackerEnabled",   true);

        QteTracker.qteLuckEnabled      = getBool(obj,  "qteLuckEnabled",      true);
        QteTracker.qteExpEnabled       = getBool(obj,  "qteExpEnabled",       true);
        QteTracker.qteChanceEnabled    = getBool(obj,  "qteChanceEnabled",    true);
        QteTracker.qteCoinsEnabled     = getBool(obj,  "qteCoinsEnabled",     true);
        QteTracker.qteTickSkipEnabled  = getBool(obj,  "qteTickSkipEnabled",  true);
        ChatFilter.chatFilterEnabled                = getBool(obj,  "chatFilterEnabled",               false);
        ChatFilter.filterManaMeteor                = getBool(obj,  "filterManaMeteor",                true);
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
    }

    public static void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("chatUpdatesEnabled",               IslesClient.chatUpdatesEnabled);
        obj.addProperty("modOnlySoundsEnabled",             IslesClient.modOnlySoundsEnabled);
        obj.addProperty("inventoryFullNotifyEnabled",       InventoryNotifier.inventoryFullNotifyEnabled);
        obj.addProperty("inventoryFullVolume",              InventoryNotifier.inventoryFullVolume);
        obj.addProperty("dropNotifyEnabled",             DropNotifier.dropNotifyEnabled);
        obj.addProperty("dropNotifyVolume",              DropNotifier.dropNotifyVolume);
        obj.addProperty("depletionPingEnabled",          NodeAlertManager.depletionPingEnabled);
        obj.addProperty("depletionPingVolume",           NodeAlertManager.depletionPingVolume);
        obj.addProperty("regenPingVolume",               NodeAlertManager.regenPingVolume);
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
        obj.addProperty("harvestTimerEnabled",           HarvestTimer.harvestTimerEnabled);
        obj.addProperty("qteTrackerEnabled",    QteTracker.qteTrackerEnabled);

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
}
