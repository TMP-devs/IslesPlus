package com.islesplus.hud;

import com.islesplus.features.bosstracker.BossTimerHud;
import com.islesplus.features.grounditemsnotifier.GroundItemsHudRenderer;
import com.islesplus.features.inventorynotifier.InventoryNotifier;
import com.islesplus.features.inventorysearch.InventorySearch;
import com.islesplus.features.plushiefinder.PlushieStatusHudRenderer;
import com.islesplus.features.qtetracker.QteHudRenderer;
import com.islesplus.features.rankcalculator.RankHudRenderer;

import java.util.List;

/** Every element the HUD editor can move and scale, in drawing order (later ones on top). */
public final class HudElements {
    private HudElements() {}

    private static List<HudElement> all;

    public static List<HudElement> all() {
        // Built on first use, not in a static initialiser: the elements live in the feature classes.
        if (all == null) {
            all = List.of(
                RankHudRenderer.ELEMENT,
                PlushieStatusHudRenderer.ELEMENT,
                BossTimerHud.ELEMENT,
                com.islesplus.features.foodbuff.FoodBuffTimer.ELEMENT,
                com.islesplus.features.voidrift.VoidRiftTimer.CORNER_ELEMENT,
                com.islesplus.features.eggtimer.EggTimer.ROW_ELEMENT,
                com.islesplus.features.eggtimer.EggTimer.WARNING_ELEMENT,
                QteHudRenderer.ELEMENT,
                GroundItemsHudRenderer.ELEMENT,
                InventoryNotifier.ELEMENT,
                com.islesplus.features.berryalert.BerryAlert.ELEMENT,
                com.islesplus.features.voidrift.VoidRiftTimer.ALERT_ELEMENT,
                InventorySearch.ELEMENT);
        }
        return all;
    }
}
