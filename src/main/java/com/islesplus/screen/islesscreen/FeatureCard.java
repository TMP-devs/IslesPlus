package com.islesplus.screen.islesscreen;

import net.minecraft.item.Item;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class FeatureCard {
    enum ControlType {
        TOGGLE,
        EXPANDABLE
    }

    final String label;
    final String description;
    String note = null; // optional small warning rendered below description
    String killedKey = null; // optional remote kill-switch key
    String[] legendLines = null; // optional text lines shown when expanded
    final Supplier<Item> iconSupplier;
    final BooleanSupplier enabledSupplier;
    final Consumer<Boolean> toggleAction;
    final boolean redDescription;
    final ControlType controlType;
    final String[] expandOptions;
    final BooleanSupplier[] optionSuppliers;
    final Runnable[] optionTogglers;
    final String sliderLabel;
    final Supplier<Float> sliderSupplier;
    final Consumer<Float> sliderConsumer;
    final boolean huePicker;

    FeatureCard(
        String label,
        String description,
        Supplier<Item> iconSupplier,
        BooleanSupplier enabledSupplier,
        Consumer<Boolean> toggleAction
    ) {
        this.label = label;
        this.description = description;
        this.iconSupplier = iconSupplier;
        this.enabledSupplier = enabledSupplier;
        this.toggleAction = toggleAction;
        this.redDescription = false;
        this.controlType = ControlType.TOGGLE;
        this.expandOptions = null;
        this.optionSuppliers = null;
        this.optionTogglers = null;
        this.sliderLabel = null;
        this.sliderSupplier = null;
        this.sliderConsumer = null;
        this.huePicker = false;
    }

    FeatureCard(
        String label,
        String description,
        Supplier<Item> iconSupplier,
        BooleanSupplier enabledSupplier,
        Consumer<Boolean> toggleAction,
        boolean redDescription
    ) {
        this.label = label;
        this.description = description;
        this.iconSupplier = iconSupplier;
        this.enabledSupplier = enabledSupplier;
        this.toggleAction = toggleAction;
        this.redDescription = redDescription;
        this.controlType = ControlType.TOGGLE;
        this.expandOptions = null;
        this.optionSuppliers = null;
        this.optionTogglers = null;
        this.sliderLabel = null;
        this.sliderSupplier = null;
        this.sliderConsumer = null;
        this.huePicker = false;
    }

    FeatureCard(
        String label,
        String description,
        Supplier<Item> iconSupplier,
        BooleanSupplier enabledSupplier,
        Consumer<Boolean> toggleAction,
        String[] expandOptions,
        BooleanSupplier[] optionSuppliers,
        Runnable[] optionTogglers,
        String sliderLabel,
        Supplier<Float> sliderSupplier,
        Consumer<Float> sliderConsumer,
        boolean huePicker
    ) {
        this.label = label;
        this.description = description;
        this.iconSupplier = iconSupplier;
        this.enabledSupplier = enabledSupplier;
        this.toggleAction = toggleAction;
        this.redDescription = false;
        this.controlType = ControlType.EXPANDABLE;
        this.expandOptions = expandOptions;
        this.optionSuppliers = optionSuppliers;
        this.optionTogglers = optionTogglers;
        this.sliderLabel = sliderLabel;
        this.sliderSupplier = sliderSupplier;
        this.sliderConsumer = sliderConsumer;
        this.huePicker = huePicker;
    }

    FeatureCard withKilledKey(String key) {
        this.killedKey = key;
        return this;
    }

    FeatureCard withLegend(String... lines) {
        this.legendLines = lines;
        return this;
    }

    boolean isKilled() {
        return this.killedKey != null && com.islesplus.sync.FeatureFlags.isKilled(this.killedKey);
    }

    void toggle() {
        if (this.toggleAction == null || this.enabledSupplier == null || isKilled()) {
            return;
        }
        this.toggleAction.accept(!this.enabledSupplier.getAsBoolean());
    }
}
