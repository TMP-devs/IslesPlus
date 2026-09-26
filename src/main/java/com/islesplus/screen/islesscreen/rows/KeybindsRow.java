package com.islesplus.screen.islesscreen.rows;

import com.islesplus.IslesClient;
import com.islesplus.features.superjump.SuperJump;
import com.islesplus.screen.islesscreen.FeatureRow;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.FootnoteBox;
import com.islesplus.ui.widgets.KeyChip;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.Panel;
import net.minecraft.client.option.KeyBinding;

/**
 * Keybinds row: a toggle-less card whose drawer lists every Isles+ keybind as a bevelled strip
 * (label + {@link KeyChip}), plus a footnote pointing at the vanilla controls screen.
 */
public final class KeybindsRow {
    private static final int ROW_GAP = 4;

    private KeybindsRow() {}

    public static FeatureRow build() {
        Flow.Column column = new Flow.Column(3)
            .add(strip("Lock Slot", IslesClient.LOCK_SLOT_KEY))
            .add(strip("Confirm Full Inventory", IslesClient.CONFIRM_INVENTORY_FULL_KEY))
            .add(strip("Open Backpack (/bp)", IslesClient.BACKPACK_KEY))
            .add(strip("Open Trash (/trash)", IslesClient.TRASH_KEY))
            .add(strip("Open Resource Vault", IslesClient.RESOURCE_VAULT_KEY))
            .add(strip("Cosmetics Hall (/cosmeticshall)", IslesClient.COSMETICS_HALL_KEY))
            .add(strip("Boss Timers (/bossary)", IslesClient.BOSSARY_KEY))
            .add(strip("Auto Party", IslesClient.AUTO_PARTY_KEY))
            .add(strip("Party Warp (/p warp)", IslesClient.PARTY_WARP_KEY))
            .add(superJumpStrip())
            .add(new FootnoteBox("Configure: Options › Controls › Isles+"));

        return new FeatureRow("Keybinds", "Available Isles+ keybinds.")
            .tooltipKey("keybinds")
            .drawer(column);
    }

    private static final String SUPER_JUMP_NOTE = "We don't recommend binding this to your jump or off hand key: "
        + "you would lose the ability to jump or dash normally and could only super dash.";

    /** Super Jump carries an info note and warns when one of the two keys it presses is unbound.
     * The note is the "super_jump" tooltip in features_v2.json when there is one ("" hides the mark). */
    private static Widget superJumpStrip() {
        InfoMark mark = new InfoMark(() -> FeatureFlags.tooltip("super_jump", SUPER_JUMP_NOTE));
        Widget row = new Flow.WrapRow(ROW_GAP, ROW_GAP) {
            @Override public int layout(int x, int y, int width) {
                mark.visible = FeatureFlags.tooltip("super_jump", SUPER_JUMP_NOTE) != null;
                return super.layout(x, y, width);
            }
        }
            .add(new Label("Super Jump (off hand + jump)", Theme.TEXT_STRONG, Fonts.BODY).wrap())
            .add(mark)
            .add(new KeyConflictMark(IslesClient.SUPER_JUMP_KEY).alsoWarn(SuperJump::unboundWarning))
            .add(new KeyChip(IslesClient.SUPER_JUMP_KEY));
        return new Panel(row, 4, Theme.SURFACE, Theme.RAISED, Theme.SECONDARY, Theme.SURFACE_RING);
    }

    private static Widget strip(String label, KeyBinding binding) {
        Widget row = new Flow.WrapRow(ROW_GAP, ROW_GAP)
            .add(new Label(label, Theme.TEXT_STRONG, Fonts.BODY).wrap())
            .add(new KeyConflictMark(binding))
            .add(new KeyChip(binding));
        return new Panel(row, 4, Theme.SURFACE, Theme.RAISED, Theme.SECONDARY, Theme.SURFACE_RING);
    }
}
