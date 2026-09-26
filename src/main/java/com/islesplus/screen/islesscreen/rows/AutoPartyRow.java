package com.islesplus.screen.islesscreen.rows;

import com.islesplus.IslesClient;
import com.islesplus.IslesPlusConfig;
import com.islesplus.features.autoparty.AutoParty;
import com.islesplus.features.autoparty.AutoParty.PartyGroup;
import com.islesplus.features.autoparty.PartyGroups;
import com.islesplus.screen.islesscreen.FeatureRow;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.ConfirmDialog;
import com.islesplus.ui.widgets.Dropdown;
import com.islesplus.ui.widgets.EmptyBox;
import com.islesplus.ui.widgets.SmallToggle;
import com.islesplus.ui.widgets.TextField;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.List;

/**
 * Auto Party row: a toggle plus a drawer holding the auto-leave/disband toggle, a group pager
 * bar ({@link PagerBar}) and a dynamic list of per-member text fields for whichever group is
 * active.
 *
 * <p>The member list is rebuilt from scratch (in {@link DrawerBody#layout}) whenever the active
 * group's identity or member count has changed since the last build, the same kind of
 * change-detection {@link GroundItemsRow} and {@link BossTimersRow} use for their dynamic lists.
 */
public final class AutoPartyRow {
    private static final int ROW_GAP = 4;

    private AutoPartyRow() {}

    public static FeatureRow build(OverlayHost host) {
        // ensureGroup() usually finds the config already in order and changes nothing; saving
        // unconditionally would rewrite the config file on every single /ip open.
        int groupsBefore = AutoParty.groups.size();
        int activeBefore = AutoParty.activeGroupIdx;
        AutoParty.activeGroupIdx = PartyGroups.ensureGroup(AutoParty.groups, AutoParty.friends, AutoParty.activeGroupIdx);
        if (AutoParty.groups.size() != groupsBefore || AutoParty.activeGroupIdx != activeBefore) {
            IslesPlusConfig.save();
        }
        return new FeatureRow("Auto Party", "One keybind to invite your party.")
            .killedKey("auto_party")
            .toggle(() -> AutoParty.enabled, v -> { AutoParty.enabled = v; IslesPlusConfig.save(); })
            .drawer(new DrawerBody(host));
    }

    private static PartyGroup activeGroup() {
        List<PartyGroup> groups = AutoParty.groups;
        int idx = AutoParty.activeGroupIdx;
        return (idx >= 0 && idx < groups.size()) ? groups.get(idx) : null;
    }

    /** Drawer body: static auto-leave/disband toggle + pager bar, a per-member field list
     * rebuilt when the active group or its member count changes, and the add/new-group actions. */
    private static final class DrawerBody extends Widget {
        private final Flow.Column column = new Flow.Column(Metrics.DRAWER_GAP);
        private final Flow.Column memberColumn = new Flow.Column(ROW_GAP);

        private PartyGroup builtFromGroup;
        private int builtFromSize = -1;
        private boolean focusNewMember;

        DrawerBody(OverlayHost host) {
            SmallToggle autoLeave = new SmallToggle("Auto leave / disband",
                () -> AutoParty.autoLeaveDisband,
                () -> { AutoParty.autoLeaveDisband = !AutoParty.autoLeaveDisband; IslesPlusConfig.save(); });

            Flow.WrapRow actions = new Flow.WrapRow(ROW_GAP, ROW_GAP).align(Flow.Align.END)
                .add(new Button("Add friend", Button.Kind.PRIMARY, this::addFriend).plus())
                .add(new Button("New group", Button.Kind.SECONDARY, this::newGroup).plus());

            column.add(autoLeave)
                .add(new PagerBar(host))
                .add(memberColumn)
                .add(actions);
        }

        private void addFriend() {
            PartyGroup group = activeGroup();
            if (group == null) return;
            group.members.add("");
            IslesPlusConfig.save();
            focusNewMember = true;
        }

        private void newGroup() {
            AutoParty.activeGroupIdx = PartyGroups.addGroup(AutoParty.groups);
            IslesPlusConfig.save();
        }

        private void removeMember(PartyGroup group, int index) {
            if (index < group.members.size()) {
                group.members.remove(index);
                IslesPlusConfig.save();
            }
        }

        private boolean structurallyChanged(PartyGroup group) {
            return group != builtFromGroup || group.members.size() != builtFromSize;
        }

        private void rebuildMembers(PartyGroup group) {
            memberColumn.clear();
            List<String> members = group.members;
            TextField last = null;
            if (members.isEmpty()) {
                memberColumn.add(new EmptyBox("No one in this group yet"));
            } else {
                for (int i = 0; i < members.size(); i++) {
                    final int idx = i;
                    TextField field = new TextField(
                        () -> idx < group.members.size() ? group.members.get(idx) : "",
                        v -> { if (idx < group.members.size()) group.members.set(idx, v); },
                        "player name", 16)
                        .onBlur(IslesPlusConfig::save)
                        .noSpaces()
                        .ring(Theme.SURFACE_RING);
                    Button remove = new Button("Remove", Button.Kind.ICON, () -> removeMember(group, idx));
                    memberColumn.add(new Flow.WrapRow(ROW_GAP, ROW_GAP).add(field).add(remove));
                    last = field;
                }
            }
            builtFromGroup = group;
            builtFromSize = members.size();
            if (focusNewMember && last != null) last.focus();
            focusNewMember = false;
        }

        @Override public int prefWidth() { return Integer.MAX_VALUE; }

        @Override public int layout(int x, int y, int width) {
            PartyGroup group = activeGroup();
            if (group != null && structurallyChanged(group)) rebuildMembers(group);

            this.x = x;
            this.y = y;
            this.w = width;
            this.h = column.layout(x, y, width);
            return this.h;
        }

        @Override public void render(DrawContext ctx, int mouseX, int mouseY) { column.render(ctx, mouseX, mouseY); }
        @Override public boolean mouseClicked(double mx, double my, int button) { return column.mouseClicked(mx, my, button); }
        @Override public boolean mouseDragged(double mx, double my) { return column.mouseDragged(mx, my); }
        @Override public void mouseReleased() { column.mouseReleased(); }
        @Override public boolean mouseScrolled(double mx, double my, double amount) { return column.mouseScrolled(mx, my, amount); }
        @Override public boolean keyPressed(KeyInput in) { return column.keyPressed(in); }
        @Override public boolean charTyped(CharInput in) { return column.charTyped(in); }
        @Override public void unfocus() { column.unfocus(); }
    }

    /** RAISED-bevelled 18-high bar: a Dropdown over the party groups (or, while renaming, a
     * TextField for the draft name) filling the left part, then an EDIT/OK segment sized to its label plus padding
     * and a fixed 16-wide delete segment. Forwards every input hook to whichever left widget is
     * currently showing; only responds to left clicks. */
    private static final class PagerBar extends Widget {
        private static final int HEIGHT = 18;
        private static final int EDIT_PAD = 5;
        private static final int DELETE_W = 16;

        private final OverlayHost host;
        private final Dropdown dropdown;
        private final TextField renameField;
        private boolean renaming = false;
        private String draft = "";

        private int editX, deleteX, leftW;

        /** Wide enough for "Edit" (the longer label) with EDIT_PAD either side, so OK does not resize the bar. */
        private static int editW() { return Fonts.width("Edit", Fonts.SMALL) + EDIT_PAD * 2; }

        PagerBar(OverlayHost host) {
            this.host = host;
            dropdown = new Dropdown(host, Dropdown.Style.LIGHT,
                () -> AutoParty.groups.size(),
                i -> AutoParty.groups.get(i).name,
                i -> PartyGroups.memberCount(AutoParty.groups.get(i)),
                () -> AutoParty.activeGroupIdx,
                i -> { AutoParty.activeGroupIdx = i; IslesPlusConfig.save(); });

            renameField = new TextField(() -> draft, v -> draft = v, "", 24)
                .onCommit(this::commitRename)
                .onCancel(() -> renaming = false);
        }

        private void commitRename() {
            PartyGroup group = activeGroup();
            if (group != null) {
                PartyGroups.rename(AutoParty.groups, AutoParty.activeGroupIdx, draft);
                IslesPlusConfig.save();
            }
            renaming = false;
            renameField.unfocus();
        }

        private void startRename() {
            PartyGroup group = activeGroup();
            if (group == null) return;
            draft = group.name;
            renaming = true;
            renameField.focus();
        }

        /** Deleting a group throws away its player list for good, so ask first. */
        private void confirmDelete() {
            PartyGroup group = activeGroup();
            if (group == null) return;
            int players = group.members.size();
            String message = "Delete the group \"" + group.name + "\" and its " + players
                + (players == 1 ? " player" : " players") + "? This cannot be undone.";
            ConfirmDialog.open(host, "Delete group", message, "Delete", this::deleteActive);
        }

        private void deleteActive() {
            AutoParty.activeGroupIdx = PartyGroups.deleteGroup(AutoParty.groups, AutoParty.activeGroupIdx);
            IslesPlusConfig.save();
            renaming = false;
            renameField.unfocus();
        }

        @Override public int prefWidth() { return Integer.MAX_VALUE; }

        @Override public int layout(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.w = width;
            this.h = HEIGHT;

            deleteX = x + width - DELETE_W;
            editX = deleteX - editW();
            leftW = Math.max(1, editX - x);

            if (renaming) {
                renameField.layout(x, y + (HEIGHT - Metrics.FIELD_H) / 2, leftW);
            } else {
                dropdown.layout(x, y, leftW);
            }
            return this.h;
        }

        @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
            Draw.bevel(ctx, x, y, w, h, Theme.RAISED, Theme.RAISED_LIT, Theme.RAISED_SHADE, Theme.SURFACE_RING);

            if (renaming) renameField.render(ctx, mouseX, mouseY);
            else dropdown.render(ctx, mouseX, mouseY);

            renderSegment(ctx, mouseX, mouseY, editX, editW(), renaming ? "OK" : "Edit", false);
            renderSegment(ctx, mouseX, mouseY, deleteX, DELETE_W, null, true);
        }

        private void renderSegment(DrawContext ctx, int mouseX, int mouseY, int segX, int segW, String label, boolean isDelete) {
            boolean hover = mouseX >= segX && mouseX < segX + segW && mouseY >= y && mouseY < y + h;
            int fillC = isDelete ? (hover ? Theme.OXBLOOD : Theme.SECONDARY) : (hover ? Theme.SECONDARY_HOVER : Theme.SECONDARY);
            ctx.fill(segX, y, segX + segW, y + h, fillC);
            ctx.fill(segX, y, segX + 1, y + h, Theme.SECONDARY_SHADE);
            if (isDelete) {
                Draw.cross(ctx, segX + segW / 2, y + h / 2, hover ? Theme.CREAM : Theme.TEXT_LABEL);
            } else {
                int textH = Fonts.height(Fonts.SMALL);
                Fonts.drawCentered(ctx, label, segX + segW / 2, y + (h - textH) / 2, Theme.TEXT_LABEL, Fonts.SMALL);
            }
        }

        @Override public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !visible || !contains(mx, my)) return false;
            if (mx >= deleteX) {
                IslesClient.playMenuClickSound();
                confirmDelete();
                return true;
            }
            if (mx >= editX) {
                IslesClient.playMenuClickSound();
                if (renaming) commitRename(); else startRename();
                return true;
            }
            if (!renaming) return dropdown.mouseClicked(mx, my, button);
            if (renameField.mouseClicked(mx, my, button)) return true;
            // Inside the bar but outside the field itself (the 1px band above and below it):
            // treat it as a blur so the draft is committed rather than left dangling.
            unfocus();
            return true;
        }

        @Override public boolean mouseDragged(double mx, double my) {
            return renaming ? renameField.mouseDragged(mx, my) : dropdown.mouseDragged(mx, my);
        }

        @Override public void mouseReleased() {
            renameField.mouseReleased();
            dropdown.mouseReleased();
        }

        @Override public boolean mouseScrolled(double mx, double my, double amount) {
            return renaming ? renameField.mouseScrolled(mx, my, amount) : dropdown.mouseScrolled(mx, my, amount);
        }

        @Override public boolean keyPressed(KeyInput in) {
            return renaming && renameField.keyPressed(in);
        }

        @Override public boolean charTyped(CharInput in) {
            return renaming && renameField.charTyped(in);
        }

        /** Commit-on-blur. A click anywhere else in the drawer reaches this bar as unfocus()
         * (the container's pre-pass over children that do not contain the click point, or its
         * sweep after a sibling consumes it), which is the signal that the rename is over.
         * Enter, OK and Escape have all already cleared {@code renaming} by the time they get
         * here, Escape via the field's onCancel, so a cancelled rename is never committed,
         * which also makes this idempotent, as unfocus() must be. */
        @Override public void unfocus() {
            if (renaming) commitRename();
            renameField.unfocus();
            dropdown.unfocus();
        }
    }
}
