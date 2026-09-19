package com.islesplus.entity;

/** which skill a node belongs to, read off the "N <Skill> Power" line on its label */
public enum NodeSkill {
    MINING, FARMING, WOODCUTTING, FISHING;

    /** true for skills whose tick skip is a block_display rig (mining ore box, farming crop) */
    public boolean usesBlockTickSkip() {
        return this == MINING || this == FARMING;
    }

    /** normalized (lowercase, single spaced) label text -> skill, or null if it's not on there */
    public static NodeSkill fromLabel(String normalizedText) {
        if (normalizedText == null) return null;
        if (normalizedText.contains("mining power")) return MINING;
        if (normalizedText.contains("farming power")) return FARMING;
        if (normalizedText.contains("woodcutting power")) return WOODCUTTING;
        if (normalizedText.contains("fishing power")) return FISHING;
        return null;
    }
}
