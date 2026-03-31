package com.islesplus.features.autoparty;

import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class AutoParty {

    public static boolean enabled = false;
    public static boolean autoLeaveDisband = true;
    public static final List<String> friends = new ArrayList<>();

    // Party groups
    public static final List<PartyGroup> groups = new ArrayList<>();
    public static int activeGroupIdx = -1; // -1 = use flat friends list

    public static class PartyGroup {
        public String name;
        public final List<String> members = new ArrayList<>();
        public PartyGroup(String name) { this.name = name; }
    }

    private static final Deque<ScheduledCommand> queue = new ArrayDeque<>();

    private static final class ScheduledCommand {
        final String command;
        final long executeAtMs;
        ScheduledCommand(String command, long executeAtMs) {
            this.command = command;
            this.executeAtMs = executeAtMs;
        }
    }

    private AutoParty() {}

    /** Get the active invite list: group members if groups exist, otherwise flat friends. */
    public static List<String> getActiveInviteList() {
        if (!groups.isEmpty() && activeGroupIdx >= 0 && activeGroupIdx < groups.size()) {
            return groups.get(activeGroupIdx).members;
        }
        return friends;
    }

    public static void trigger(MinecraftClient client) {
        if (client.player == null || FeatureFlags.isKilled("auto_party")) return;
        queue.clear();
        long now = Util.getMeasuringTimeMs();
        long inviteAt = now;
        if (autoLeaveDisband) {
            queue.add(new ScheduledCommand("party leave",   now));
            queue.add(new ScheduledCommand("party disband", now + 600));
            inviteAt = now + 1200;
        }
        for (String friend : getActiveInviteList()) {
            if (!friend.isBlank()) {
                queue.add(new ScheduledCommand("party invite " + friend.strip(), inviteAt));
                inviteAt += 600;
            }
        }
    }

    public static void tick(MinecraftClient client) {
        if (queue.isEmpty() || client.player == null || FeatureFlags.isKilled("auto_party")) return;
        long now = Util.getMeasuringTimeMs();
        ScheduledCommand next = queue.peek();
        if (next != null && now >= next.executeAtMs) {
            queue.poll();
            client.player.networkHandler.sendChatCommand(next.command);
        }
    }
}
