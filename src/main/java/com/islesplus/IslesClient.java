package com.islesplus;

import com.islesplus.IslesPlusConfig;
import com.islesplus.entity.EntityScanResult;
import com.islesplus.entity.EntityScanner;
import com.islesplus.features.chestfinder.ChestFinder;
import com.islesplus.features.voidcrystalfinder.VoidCrystalFinder;
import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.dropnotifier.DropNotifier;
import com.islesplus.features.inventorynotifier.InventoryNotifier;
import com.islesplus.features.mobfinder.MobFinder;
import com.islesplus.features.nodealertmanager.NodeAlertManager;
import com.islesplus.features.nodealertmanager.NodeTracker;
import com.islesplus.features.harvesttimer.HarvestTimer;
import com.islesplus.features.harvesttimer.NerdModeActivator;
import com.islesplus.features.noderadius.NodeRadiusRenderer;
import com.islesplus.features.playerfinder.PlayerFinder;
import com.islesplus.features.plushiefinder.PlushieFinder;
import com.islesplus.features.nodealertmanager.NodeRepository;
import com.islesplus.features.rankcalculator.RankCalculator;
import com.islesplus.features.rankcalculator.RankHudRenderer;
import com.islesplus.features.rankcalculator.RiftRepository;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier;
import com.islesplus.features.grounditemsnotifier.GroundItemsRenderer;
import com.islesplus.features.grounditemsnotifier.GroundItemsHudRenderer;
import com.islesplus.features.qtetracker.QteHudRenderer;
import com.islesplus.features.qtetracker.QteTracker;
import com.islesplus.features.qtetracker.QteRenderer;
import com.islesplus.features.secretfinder.SecretBlockRenderer;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.features.waystonefinder.WaystoneFinder;
import com.islesplus.features.waystonefinder.WaystoneTagRenderer;
import com.islesplus.features.plushiefinder.PlushieMenuHook;
import com.islesplus.features.plushiefinder.PlushieRepository;
import com.islesplus.features.plushiefinder.PlushieStatusHudRenderer;
import com.islesplus.features.plushiefinder.PlushieWaypointRenderer;
import com.islesplus.features.bosstracker.BossaryHook;
import com.islesplus.features.autoparty.AutoParty;
import com.islesplus.features.bosstracker.BossTimerHud;
import com.islesplus.features.bosstracker.BossTracker;
import com.islesplus.features.resourcevault.ResourceVaultOpener;
import com.islesplus.features.slotlocker.SlotLocker;
import com.islesplus.features.superjump.SuperJump;
import com.islesplus.world.WorldIdentification;
import com.islesplus.mixin.HandledScreenAccessor;
import com.islesplus.screen.islesscreen.IslesScreen;
import com.islesplus.sound.SoundController;
import com.islesplus.sound.ModSounds;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.sync.RefreshPoller;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IslesClient implements ClientModInitializer {
    private static final KeyBinding.Category KEYBIND_CATEGORY_ISLESPLUS = KeyBinding.Category.create(
        Identifier.of("islesplus", "a_islesplus")
    );
    // Chat palette, from the MOTD mockup. Every line is one colour throughout (diamond, text, arrow,
    // link): cream for ordinary lines, the MOTD's own accent, lifted oxblood for "update available".
    static final int   CHAT_TEXT   = Theme.WELL_ROW_TEXT & 0xFFFFFF;   // cream #e8ddc6
    static final int   CHAT_BRAND  = Theme.HUD_TITLE     & 0xFFFFFF;   // lifted oxblood: wordmark, update line
    static final int   CHAT_PLUS   = Theme.RAISED        & 0xFFFFFF;   // tan plus
    static final int   CHAT_ACCENT = Theme.HUD_VERDIGRIS & 0xFFFFFF;   // default MOTD colour
    static final float ALERT_VOLUME = 2.0F;
    static final float ALERT_PITCH  = 1.0F;

    public static final KeyBinding LOCK_SLOT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.lock_slot",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding CONFIRM_INVENTORY_FULL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.confirm_inventory_full",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding BACKPACK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.backpack",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding TRASH_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.trash",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding RESOURCE_VAULT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.resource_vault",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding AUTO_PARTY_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.auto_party",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding PARTY_WARP_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.party_warp",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding COSMETICS_HALL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.cosmetics_hall",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    public static final KeyBinding SUPER_JUMP_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.super_jump",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));

    public static boolean chatUpdatesEnabled = false;
    public static boolean modOnlySoundsEnabled = false;
    /** true while joining through our title screen button, ConfirmScreenMixin uses it to auto accept the resource pack */
    public static volatile boolean connectingToIsles = false;
    public static boolean lockSlotKeyHeld = false;

    @Override
    public void onInitializeClient() {
        IslesPlusConfig.load();
        PlushieRepository.init();
        RiftRepository.init();
        NodeRepository.init();
        FeatureFlags.init();

        // has to go before the main BEFORE_INIT block so its allowKeyPress
        // runs first and can eat character keys while the search bar is focused
        InventorySearch.register();

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen<?> handledScreen)) return;

            NerdModeActivator.onScreenOpen(handledScreen);
            ResourceVaultOpener.onScreenOpen(handledScreen);
            BossaryHook.onScreenOpen(handledScreen);
            ScreenEvents.afterTick(screen).register(s -> {
                NerdModeActivator.onScreenTick((HandledScreen<?>) s);
                ResourceVaultOpener.onScreenTick((HandledScreen<?>) s);
                BossaryHook.onScreenTick((HandledScreen<?>) s);
            });

            ScreenMouseEvents.allowMouseClick(screen).register((s, context) -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) return true;
                Slot focused = ((HandledScreenAccessor) s).getFocusedSlot();
                if (focused == null) return true;
                return !SlotLocker.isLocked(focused, mc.player.getInventory());
            });

            ScreenKeyboardEvents.allowKeyPress(screen).register((s, context) -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) return true;

                // L key: toggle lock on the focused slot
                if (LOCK_SLOT_KEY.matchesKey(context)) {
                    if (lockSlotKeyHeld) {
                        return false;
                    }
                    lockSlotKeyHeld = true;
                    Slot focused = ((HandledScreenAccessor) s).getFocusedSlot();
                    if (focused != null) {
                        if (SlotLocker.toggleLock(focused, mc.player.getInventory())) {
                            playSlotLockDing(mc);
                        }
                    }
                    return false; // consume the key press
                }

                // hotbar swap keys 1-9, block if either slot is locked
                int hotbarIndex = -1;
                for (int i = 0; i < 9; i++) {
                    if (mc.options.hotbarKeys[i].matchesKey(context)) {
                        hotbarIndex = i;
                        break;
                    }
                }
                if (hotbarIndex != -1) {
                    Slot focused = ((HandledScreenAccessor) s).getFocusedSlot();
                    if (focused != null && SlotLocker.isLocked(focused, mc.player.getInventory())) {
                        return false;
                    }
                    if (SlotLocker.isHotbarSlotLocked(hotbarIndex)) {
                        return false;
                    }
                }

                return true;
            });

            ScreenKeyboardEvents.allowKeyRelease(screen).register((s, context) -> {
                if (LOCK_SLOT_KEY.matchesKey(context)) {
                    lockSlotKeyHeld = false;
                    return false;
                }
                return true;
            });
        });

        PlushieMenuHook.register();
        com.islesplus.screen.TitleMenuRestyle.register();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            PlushieFinder.onMessage(text);
            HarvestTimer.onMessage(text);
            BossTracker.onChatMessage(text);
            RankCalculator.onChatMessage(text);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String text = message.getString();
            PlushieFinder.onMessage(text);
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            resetRuntimeState();
            WorldIdentification.onJoin();
            BossTracker.onWorldJoin();
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            RefreshPoller.stop();
            resetRuntimeState();
        }));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("rvault")
                .executes(context -> { ResourceVaultOpener.activate(); return 1; })
            );
            String[] commandAliases = {"islesplus", "ip", "iqol"};
            for (String alias : commandAliases) {
                dispatcher.register(ClientCommandManager.literal(alias)
                    .executes(context -> {
                        MinecraftClient client2 = MinecraftClient.getInstance();
                        client2.execute(() -> client2.setScreen(new IslesScreen()));
                        return 1;
                    })
                    .then(ClientCommandManager.literal("party")
                        .executes(context -> {
                            MinecraftClient client2 = MinecraftClient.getInstance();
                            client2.execute(() -> AutoParty.trigger(client2));
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("rift")
                        .then(ClientCommandManager.literal("setplayers")
                            .then(ClientCommandManager.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                .executes(context -> {
                                    int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count");
                                    MinecraftClient client2 = MinecraftClient.getInstance();
                                    client2.execute(() -> {
                                        if (client2.player == null) return;
                                        if (WorldIdentification.world != com.islesplus.world.PlayerWorld.RIFT) {
                                            sendInfoMessage(client2, "/rift setplayers only works inside a Rift.");
                                            return;
                                        }
                                        RankCalculator.playerCount = count;
                                        sendInfoMessage(client2, "Player count set to " + count + ".");
                                    });
                                    return 1;
                                })
                            )
                        )
                    )
                );
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(PlushieWaypointRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(WaystoneTagRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(SecretBlockRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(QteRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(GroundItemsRenderer::render);
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            RankHudRenderer.render(context, mc);
            InventoryNotifier.renderHud(context, mc);
            PlushieStatusHudRenderer.render(context, mc);
            QteHudRenderer.render(context, mc);
            GroundItemsHudRenderer.render(context, mc);
            BossTimerHud.render(context, mc);
        });
    }

    private void onClientTick(MinecraftClient client) {
        while (CONFIRM_INVENTORY_FULL_KEY.wasPressed()) { InventoryNotifier.confirm(); }
        while (BACKPACK_KEY.wasPressed()) { if (client.player != null) client.player.networkHandler.sendChatCommand("bp"); }
        while (TRASH_KEY.wasPressed()) { if (client.player != null) client.player.networkHandler.sendChatCommand("trash"); }
        while (COSMETICS_HALL_KEY.wasPressed()) { if (client.player != null) client.player.networkHandler.sendChatCommand("cosmeticshall"); }
        while (RESOURCE_VAULT_KEY.wasPressed()) { ResourceVaultOpener.activate(); }
        while (AUTO_PARTY_KEY.wasPressed()) { if (AutoParty.enabled) AutoParty.trigger(client); }
        while (PARTY_WARP_KEY.wasPressed()) { if (AutoParty.enabled && client.player != null) client.player.networkHandler.sendChatCommand("p warp"); }
        while (SUPER_JUMP_KEY.wasPressed()) { }   // drained: super jump acts on the raw key event (KeyBindingMixin)
        SuperJump.tick(client);
        EntityScanResult scan;
        try {
            scan = EntityScanner.scan(client);
        } catch (RuntimeException e) {
            reportTickFailure("entity_scanner", e);
            return; // everything below depends on the scan
        }
        guardedTick("node_tracker",              () -> NodeTracker.tick(client, scan));
        if (!FeatureFlags.isKilled("harvest_timer"))       guardedTick("harvest_timer",       HarvestTimer::tick);
        if (!FeatureFlags.isKilled("node_radius"))         guardedTick("node_radius",         () -> NodeRadiusRenderer.tick(client));
        if (!FeatureFlags.isKilled("node_depleted_ping"))  guardedTick("node_depleted_ping",  () -> NodeAlertManager.tickSkillAlert(client));
        if (!FeatureFlags.isKilled("regen_mode"))          guardedTick("regen_mode",          () -> NodeAlertManager.tickRegenReminder(client));
        if (!FeatureFlags.isKilled("drop_notify"))         guardedTick("drop_notify",         () -> DropNotifier.tick(client));
        if (!FeatureFlags.isKilled("inventory_full"))      guardedTick("inventory_full",      () -> InventoryNotifier.tick(client, CONFIRM_INVENTORY_FULL_KEY.getBoundKeyLocalizedText()));
        guardedTick("world_identification",      () -> WorldIdentification.tick(client));
        if (!FeatureFlags.isKilled("vending_machine_finder")) guardedTick("vending_machine_finder", () -> VendingMachineFinder.tick(client, scan));
        if (!FeatureFlags.isKilled("waystone_finder"))     guardedTick("waystone_finder",     () -> WaystoneFinder.tick(client, scan));
        if (!FeatureFlags.isKilled("chest_finder"))        guardedTick("chest_finder",        () -> ChestFinder.tick(client, scan));
        guardedTick("void_crystal_finder",       () -> VoidCrystalFinder.tick(client, scan));
        if (!FeatureFlags.isKilled("button_finder"))       guardedTick("button_finder",       () -> SecretFinder.tick(client, scan));
        if (!FeatureFlags.isKilled("qte_tracker"))         guardedTick("qte_tracker",         () -> QteTracker.tick(client, scan));
        if (!FeatureFlags.isKilled("mob_finder"))          guardedTick("mob_finder",          () -> MobFinder.tick(client, scan));
        if (!FeatureFlags.isKilled("player_finder"))       guardedTick("player_finder",       () -> PlayerFinder.tick(client, scan));
        if (!FeatureFlags.isKilled("rank_calculator"))     guardedTick("rank_calculator",     () -> RankCalculator.tick(client, scan));
        if (!FeatureFlags.isKilled("ground_items_notifier")) guardedTick("ground_items_notifier", () -> GroundItemsNotifier.tick(client, scan));
        if (!FeatureFlags.isKilled("boss_tracker"))        guardedTick("boss_tracker",        () -> BossTracker.tick(client));
        guardedTick("auto_party",                () -> AutoParty.tick(client));
    }

    // ---- per feature tick guard --------------------------------------------------------
    // if a tick handler throws it bubbles up into mc's tick loop and the whole game crashes.
    // one broken feature (or one weird value from the server) shouldn't take everything down,
    // so every feature ticks inside its own try/catch

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("islesplus");
    private static final java.util.Map<String, Integer> tickFailureCounts = new java.util.HashMap<>();
    private static final int TICK_FAILURE_LOG_INTERVAL = 20 * 60; // once a minute at 20 tps

    private static void guardedTick(String feature, Runnable tick) {
        try {
            tick.run();
        } catch (RuntimeException e) {
            reportTickFailure(feature, e);
        }
    }

    /** first failure per feature gets a full stack trace, after that only once a minute so we don't spam the log */
    private static void reportTickFailure(String feature, RuntimeException e) {
        int count = tickFailureCounts.merge(feature, 1, Integer::sum);
        if (count == 1) {
            LOGGER.error("[Isles+] Feature '{}' threw during client tick; it will keep ticking but this tick was skipped", feature, e);
        } else if (count % TICK_FAILURE_LOG_INTERVAL == 0) {
            LOGGER.warn("[Isles+] Feature '{}' has now failed {} ticks (latest: {})", feature, count, e.toString());
        }
    }

    public static void setChatUpdatesEnabled(boolean enabled) {
        chatUpdatesEnabled = enabled;
    }

    public static boolean shouldMuteIncomingSound(String soundId) {
        return SoundController.shouldMuteIncomingSound(soundId);
    }

    public static boolean shouldAllowLocalSoundInFocus(String soundId) {
        return SoundController.shouldAllowLocalSound(soundId);
    }

    public static void sendStatusMessage(MinecraftClient client, String message) {
        if (!chatUpdatesEnabled || client.player == null) {
            return;
        }
        sendIslesMessage(client, message);
    }

    public static void sendAlwaysMessage(MinecraftClient client, String message) {
        sendIslesMessage(client, message);
    }

    public static void sendWelcomeMessage(MinecraftClient client) {
        if (client.player == null) return;
        sendLine(client, CHAT_TEXT, "Use /ip to open the menu", "", "");

        // Join message: the styled "motd_v2" lines if the remote file has them, otherwise the old
        // plain "motd" string (its first URL becomes the link).
        List<FeatureFlags.MotdLine> lines = FeatureFlags.getMotdLines();
        if (lines.isEmpty()) {
            String motd = FeatureFlags.getMotd();
            if (motd.isEmpty()) motd = "Join our discord! https://discord.gg/UKnEWBDJ7w";
            lines = List.of(motdLineFromPlain(motd));
        }
        for (FeatureFlags.MotdLine line : lines) {
            int accent = parseColor(line.color(), CHAT_ACCENT);
            sendLine(client, accent, line.text(), line.link(), line.linkText());
        }

        String latest = FeatureFlags.getLatestVersion();
        if (!latest.isEmpty()) {
            String current = FabricLoader.getInstance().getModContainer("islesplus")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
            if (!current.isEmpty() && !current.equals(latest)) {
                String rawUrl = FeatureFlags.getLatestVersionUrl();
                String url = rawUrl.isEmpty() ? "https://modrinth.com/project/isles+" : rawUrl;
                sendLine(client, CHAT_BRAND, "Update available: " + latest, url, "");
            }
        }
    }

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    /** Old-style MOTD: the text with its first URL lifted out as the line's link. */
    static FeatureFlags.MotdLine motdLineFromPlain(String motd) {
        Matcher matcher = URL_PATTERN.matcher(motd);
        if (!matcher.find()) return new FeatureFlags.MotdLine(motd.trim(), "", "", "");
        String text = (motd.substring(0, matcher.start()) + motd.substring(matcher.end())).trim();
        // "Discord-> <url>" style leftovers: the arrow is drawn by the line itself
        text = text.replaceAll("\\s*-+>\\s*$", "").trim();
        return new FeatureFlags.MotdLine(text, matcher.group(), "", "");
    }

    /** "#rrggbb" (or "rrggbb") to RGB; anything else gives the fallback. */
    static int parseColor(String hex, int fallback) {
        if (hex == null) return fallback;
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() != 6) return fallback;
        try {
            return Integer.parseInt(h, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** "ISLES+" wordmark in Silkscreen: lifted oxblood with a tan plus (MOTD mockup). */
    private static MutableText buildIslesPrefix() {
        return Text.empty()
            .append(Fonts.of("Isles").copy().styled(s -> s.withColor(TextColor.fromRgb(CHAT_BRAND))))
            .append(Fonts.of("+").copy().styled(s -> s.withColor(TextColor.fromRgb(CHAT_PLUS))));
    }

    /**
     * One Isles+ chat line, as in the MOTD mockup: wordmark, a diamond mark, then the message, all
     * in Silkscreen. With a link it ends "-> CLICK HERE": an arrow glyph and underlined link text
     * that opens the URL.
     */
    private static void sendLine(MinecraftClient client, int textColor, String message, String link, String linkText) {
        if (client.player == null) return;
        MutableText line = Text.empty()
            .append(buildIslesPrefix())
            .append(Fonts.of(" "))
            .append(Fonts.symbol(Fonts.DIAMOND).styled(s -> s.withColor(TextColor.fromRgb(textColor))))   // always the line's own colour
            .append(Fonts.of(" " + message).copy().styled(s -> s.withColor(TextColor.fromRgb(textColor))));
        if (link != null && !link.isBlank()) {
            MutableText linkPart = Fonts.of(linkText == null || linkText.isBlank() ? "click here" : linkText).copy()
                .styled(s -> s.withColor(TextColor.fromRgb(textColor)).withUnderline(true));
            try {
                URI uri = URI.create(link);
                linkPart = linkPart.styled(s -> s.withClickEvent(new ClickEvent.OpenUrl(uri)));
            } catch (IllegalArgumentException ignored) {
                // not a usable URL: the words still show, they just do nothing
            }
            line.append(Fonts.of(" "))
                .append(Fonts.symbol(Fonts.ARROW).styled(s -> s.withColor(TextColor.fromRgb(textColor))))
                .append(Fonts.of(" "))
                .append(linkPart);
        }
        client.player.sendMessage(line, false);
    }

    /** cream line. good for one-off info messages */
    public static void sendInfoMessage(MinecraftClient client, String message) {
        sendLine(client, CHAT_TEXT, message, "", "");
    }

    private static void sendIslesMessage(MinecraftClient client, String message) {
        sendLine(client, CHAT_TEXT, message, "", "");
    }

    public static String enabledDisabled(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    public static void playMenuClickSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        ModSounds.play(client, ModSounds.Cue.MENU_CLICK);
    }

    public static void playSlotLockDing(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        ModSounds.play(client, ModSounds.Cue.SLOT_LOCK);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static void resetRuntimeState() {
        NodeTracker.reset();
        HarvestTimer.reset();
        NerdModeActivator.reset();
        BossaryHook.reset();
        NodeRadiusRenderer.reset();
        NodeAlertManager.reset();
        DropNotifier.reset();
        InventoryNotifier.reset();
        VendingMachineFinder.reset();
        WaystoneFinder.reset();
        ChestFinder.reset();
        VoidCrystalFinder.reset();
        SecretFinder.reset();
        QteTracker.reset();
        MobFinder.reset();
        PlayerFinder.reset();
        SlotLocker.reset(); // no-op by design - locks persist across sessions
        ResourceVaultOpener.reset();
        lockSlotKeyHeld = false;
        connectingToIsles = false;
        WorldIdentification.reset();
        RankCalculator.reset();
        GroundItemsNotifier.reset();
        BossTracker.reset();
        SuperJump.reset();
    }

}
