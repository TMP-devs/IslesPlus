package com.islesplus.screen.islesscreen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.islesplus.logging.IslesLog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class RiftWarningManager {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
        .resolve("islesplus").resolve("rift_warning.json");

    private static Boolean dismissed = null;

    private RiftWarningManager() {}

    static boolean isDismissed() {
        if (dismissed == null) load();
        return Boolean.TRUE.equals(dismissed);
    }

    static void setDismissed() {
        dismissed = true;
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("dismissed", true);
            Files.writeString(PATH, obj.toString());
        } catch (IOException e) {
            IslesLog.runtimeWarn("[Isles+] Failed to save rift warning state", e);
        }
    }

    private static void load() {
        dismissed = false;
        if (!Files.exists(PATH)) return;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(PATH));
            if (parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                if (root.has("dismissed")) dismissed = root.get("dismissed").getAsBoolean();
            }
        } catch (Exception ignored) {}
    }
}
