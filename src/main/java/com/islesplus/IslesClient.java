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
import com.islesplus.features.qtetracker.QteHudRenderer;
import com.islesplus.features.qtetracker.QteTracker;
import com.islesplus.features.qtetracker.QteRenderer;
import com.islesplus.features.secretfinder.SecretBlockRenderer;
import com.islesplus.features.secretfinder.SecretFinder;
import com.islesplus.features.vendingmachinefinder.VendingMachineFinder;
import com.islesplus.features.plushiefinder.PlushieMenuHook;
import com.islesplus.features.plushiefinder.PlushieRepository;
import com.islesplus.features.plushiefinder.PlushieStatusHudRenderer;
import com.islesplus.features.plushiefinder.PlushieWaypointRenderer;
import com.islesplus.features.resourcevault.ResourceVaultOpener;
import com.islesplus.features.slotlocker.SlotLocker;
import com.islesplus.world.WorldIdentification;
import com.islesplus.mixin.HandledScreenAccessor;
import com.islesplus.screen.islesscreen.IslesScreen;
import com.islesplus.sound.SoundController;
import com.islesplus.sound.ModSounds;
import com.islesplus.sync.FeatureFlags;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IslesClient implements ClientModInitializer {
    private static final KeyBinding.Category KEYBIND_CATEGORY_ISLESPLUS = KeyBinding.Category.create(
        Identifier.of("islesplus", "a_islesplus")
    );
    static final int   BRAND_DARK   = 0x390214;
    static final int   ACCENT_GOLD  = 0xD4AF37;
    static final int   STATUS_GREEN = 0x2ECC71;
    static final int   STATUS_RED   = 0xE74C3C;
    static final float ALERT_VOLUME = 2.0F;
    static final float ALERT_PITCH  = 1.0F;

    static final KeyBinding LOCK_SLOT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.lock_slot",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    static final KeyBinding CONFIRM_INVENTORY_FULL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.confirm_inventory_full",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    static final KeyBinding BACKPACK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.backpack",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    static final KeyBinding TRASH_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.trash",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));
    static final KeyBinding RESOURCE_VAULT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.islesplus.resource_vault",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN,
        KEYBIND_CATEGORY_ISLESPLUS
    ));

    public static boolean chatUpdatesEnabled = false;
    public static boolean modOnlySoundsEnabled = false;
    /** True while connecting via the "Join Skyblock Isles" title screen button; lets ConfirmScreenMixin auto-accept the resource pack. */
    public static volatile boolean connectingToIsles = false;
    public static boolean lockSlotKeyHeld = false;

    @Override
    public void onInitializeClient() {
        IslesPlusConfig.load();
        PlushieRepository.init();
        RiftRepository.init();
        NodeRepository.init();
        FeatureFlags.init();

        // Must register before the main BEFORE_INIT block so its allowKeyPress
        // handler runs first and can consume character keys when focused.
        InventorySearch.register();

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof HandledScreen<?> handledScreen)) return;

            NerdModeActivator.onScreenOpen(handledScreen);
            ResourceVaultOpener.onScreenOpen(handledScreen);
            ScreenEvents.afterTick(screen).register(s -> {
                NerdModeActivator.onScreenTick((HandledScreen<?>) s);
                ResourceVaultOpener.onScreenTick((HandledScreen<?>) s);
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

                // Hotbar swap keys (1–9): block if either endpoint is locked
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

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            PlushieFinder.onMessage(text);
            HarvestTimer.onMessage(text);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String text = message.getString();
            PlushieFinder.onMessage(text);
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            resetRuntimeState();
            WorldIdentification.onJoin();
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
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.execute(() -> client.setScreen(new IslesScreen()));
                        return 1;
                    })
                );
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(PlushieWaypointRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(SecretBlockRenderer::render);
        WorldRenderEvents.AFTER_ENTITIES.register(QteRenderer::render);
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            RankHudRenderer.render(context, mc);
            PlushieStatusHudRenderer.render(context, mc);
            QteHudRenderer.render(context, mc);
        });
    }

    private void onClientTick(MinecraftClient client) {
        while (CONFIRM_INVENTORY_FULL_KEY.wasPressed()) { InventoryNotifier.confirm(); }
        while (BACKPACK_KEY.wasPressed()) { if (client.player != null) client.player.networkHandler.sendChatCommand("bp"); }
        while (TRASH_KEY.wasPressed()) { if (client.player != null) client.player.networkHandler.sendChatCommand("trash"); }
        while (RESOURCE_VAULT_KEY.wasPressed()) { ResourceVaultOpener.activate(); }
        EntityScanResult scan = EntityScanner.scan(client);
        NodeTracker.tick(client, scan);
        if (!FeatureFlags.isKilled("harvest_timer"))       HarvestTimer.tick();
        if (!FeatureFlags.isKilled("node_radius"))         NodeRadiusRenderer.tick(client);
        if (!FeatureFlags.isKilled("node_depleted_ping"))  NodeAlertManager.tickSkillAlert(client);
        if (!FeatureFlags.isKilled("regen_mode"))          NodeAlertManager.tickRegenReminder(client);
        if (!FeatureFlags.isKilled("drop_notify"))         DropNotifier.tick(client);
        if (!FeatureFlags.isKilled("inventory_full"))      InventoryNotifier.tick(client, CONFIRM_INVENTORY_FULL_KEY.getBoundKeyLocalizedText());
        WorldIdentification.tick(client);
        if (!FeatureFlags.isKilled("vending_machine_finder")) VendingMachineFinder.tick(client, scan);
        if (!FeatureFlags.isKilled("chest_finder"))        ChestFinder.tick(client, scan);
        VoidCrystalFinder.tick(client, scan);
        if (!FeatureFlags.isKilled("button_finder"))       SecretFinder.tick(client, scan);
        if (!FeatureFlags.isKilled("qte_tracker"))       QteTracker.tick(client, scan);
        if (!FeatureFlags.isKilled("mob_finder"))          MobFinder.tick(client, scan);
        if (!FeatureFlags.isKilled("player_finder"))       PlayerFinder.tick(client, scan);
        if (!FeatureFlags.isKilled("rank_calculator"))     RankCalculator.tick(client, scan);
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
        sendIslesMessage(client, "Use /ip to open the menu.");
        String motd = FeatureFlags.getMotd();
        if (motd.isEmpty()) motd = "Join our discord! https://discord.gg/UKnEWBDJ7w";
        if (client.player != null) {
            client.player.sendMessage(Text.empty().append(buildIslesPrefix()).append(buildMotdText(motd)), false);
        }
        String latest = FeatureFlags.getLatestVersion();
        if (!latest.isEmpty() && client.player != null) {
            String current = FabricLoader.getInstance().getModContainer("islesplus")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
            if (!current.isEmpty() && !current.equals(latest)) {
                String rawUrl = FeatureFlags.getLatestVersionUrl();
                final String modrinthUrl = rawUrl.isEmpty() ? "https://modrinth.com/project/isles+" : rawUrl;
                MutableText updateMsg = Text.literal("Update available: " + latest + " - ")
                    .styled(s -> s.withColor(TextColor.fromRgb(STATUS_RED)));
                try {
                    updateMsg.append(Text.literal("click here")
                        .styled(s -> s.withColor(TextColor.fromRgb(STATUS_RED))
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(modrinthUrl)))));
                } catch (IllegalArgumentException ignored) {
                    updateMsg.append(Text.literal("click here")
                        .styled(s -> s.withColor(TextColor.fromRgb(STATUS_RED))));
                }
                client.player.sendMessage(Text.empty().append(buildIslesPrefix()).append(updateMsg), false);
            }
        }
    }

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private static Text buildMotdText(String motd) {
        Matcher matcher = URL_PATTERN.matcher(motd);
        MutableText result = Text.empty();
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                String plain = motd.substring(last, matcher.start());
                result.append(Text.literal(plain)
                    .styled(style -> style.withColor(TextColor.fromRgb(ACCENT_GOLD))));
            }
            String url = matcher.group();
            MutableText urlText = Text.literal("click here")
                .styled(style -> style.withColor(TextColor.fromRgb(ACCENT_GOLD)));
            try {
                URI uri = URI.create(url);
                urlText = urlText.styled(style -> style
                    .withUnderline(true)
                    .withClickEvent(new ClickEvent.OpenUrl(uri)));
            } catch (IllegalArgumentException ignored) {}
            result.append(urlText);
            last = matcher.end();
        }
        if (last < motd.length()) {
            result.append(Text.literal(motd.substring(last))
                .styled(style -> style.withColor(TextColor.fromRgb(ACCENT_GOLD))));
        }
        return result;
    }

    private static MutableText buildIslesPrefix() {
        return Text.empty()
            .append(Text.literal("(Isles").styled(s -> s.withColor(Formatting.DARK_GRAY)))
            .append(Text.literal("+").styled(s -> s.withColor(TextColor.fromRgb(0x55FFFF)).withBold(true)))
            .append(Text.literal(") ").styled(s -> s.withColor(Formatting.DARK_GRAY)));
    }

    private static void sendIslesMessage(MinecraftClient client, String message) {
        if (client.player == null) {
            return;
        }
        MutableText body = Text.literal(message)
            .styled(style -> style.withBold(true).withColor(TextColor.fromRgb(ACCENT_GOLD)));
        client.player.sendMessage(Text.empty().append(buildIslesPrefix()).append(body), false);
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
        NodeRadiusRenderer.reset();
        NodeAlertManager.reset();
        DropNotifier.reset();
        InventoryNotifier.reset();
        VendingMachineFinder.reset();
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
    }

}
