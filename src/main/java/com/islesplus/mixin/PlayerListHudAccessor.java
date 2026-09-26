package com.islesplus.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Comparator;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Accessor("header")
    @Nullable Text getHeader();

    @Accessor("footer")
    @Nullable Text getFooter();

    /** The order the tab list is drawn in. */
    @Accessor("ENTRY_ORDERING")
    static Comparator<PlayerListEntry> getEntryOrdering() {
        throw new AssertionError();
    }
}
