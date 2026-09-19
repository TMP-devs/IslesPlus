package com.islesplus.features.autoparty;

import java.util.List;

/**
 * Pure helpers for editing {@link AutoParty#groups}: every method operates only on the list(s)
 * passed in and returns the new active index, with no Minecraft dependencies and no side effects
 * beyond the passed list. Callers are responsible for persisting ({@code IslesPlusConfig.save()})
 * after any mutating call.
 */
public final class PartyGroups {
    private PartyGroups() {}

    /** If {@code groups} is empty, seeds it with a single "Group 1" containing a copy of
     * {@code legacyFriends} (the legacy list itself is never modified) and returns 0. Otherwise
     * clamps {@code active} into {@code [0, groups.size() - 1]}. */
    public static int ensureGroup(List<AutoParty.PartyGroup> groups, List<String> legacyFriends, int active) {
        if (groups.isEmpty()) {
            AutoParty.PartyGroup group = new AutoParty.PartyGroup("Group 1");
            group.members.addAll(legacyFriends);
            groups.add(group);
            return 0;
        }
        return Math.max(0, Math.min(active, groups.size() - 1));
    }

    /** Appends a new empty group named "Group " + (size + 1) and returns its index. */
    public static int addGroup(List<AutoParty.PartyGroup> groups) {
        groups.add(new AutoParty.PartyGroup("Group " + (groups.size() + 1)));
        return groups.size() - 1;
    }

    /** Deletes the group at {@code active}. If it was the last remaining group, it is replaced
     * by a fresh empty "Group 1" and 0 is returned; otherwise the group is removed and the new
     * active index is {@code min(active, groups.size() - 1)} (post-removal size). */
    public static int deleteGroup(List<AutoParty.PartyGroup> groups, int active) {
        if (groups.size() <= 1) {
            groups.clear();
            groups.add(new AutoParty.PartyGroup("Group 1"));
            return 0;
        }
        int idx = Math.max(0, Math.min(active, groups.size() - 1));
        groups.remove(idx);
        return Math.min(idx, groups.size() - 1);
    }

    /** Trims {@code draft} and, if non-blank, sets it as the active group's name; a blank draft
     * leaves the name unchanged. */
    public static void rename(List<AutoParty.PartyGroup> groups, int active, String draft) {
        if (draft == null) return;
        String trimmed = draft.trim();
        if (trimmed.isEmpty()) return;
        groups.get(active).name = trimmed;
    }

    /** {@code "1 member"} / {@code "n members"}. */
    public static String memberCount(AutoParty.PartyGroup g) {
        int n = g.members.size();
        return n + " member" + (n == 1 ? "" : "s");
    }
}
