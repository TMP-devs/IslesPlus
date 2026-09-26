package com.islesplus.features.nodealertmanager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeDepletedTest {
    private static boolean depleted(String label) {
        return NodeTracker.isDepleted(NodeTracker.normalizeNodeText(label), "Oak Tree");
    }

    @Test void oldLabelStillWorks() {
        assertTrue(depleted("Depleted Oak Tree\n5 Woodcutting Power"));
    }

    @Test void qualityTagInFront() {
        assertTrue(depleted("[Rich]\nDepleted Oak Tree\n5 Woodcutting Power"));
        assertTrue(depleted("[Weak] Depleted Oak Tree"));
    }

    @Test void phraseLaterInTheLabel() {
        assertTrue(depleted("[Poor]\n0x Depleted Oak Tree"));
    }

    @Test void liveNodeIsNotDepleted() {
        assertFalse(depleted("[Rich]\n12x Oak Tree\n5 Woodcutting Power"));
        assertFalse(depleted("12x Oak Tree"));
    }

    private static boolean shows(String label) {
        String n = NodeTracker.normalizeNodeText(label);
        return NodeTracker.labelShowsDepleted(n, "Oak Tree", NodeTracker.extractLeadingCount(n));
    }

    @Test void trackedLabelThatNoLongerLooksLiveIsDepleted() {
        assertTrue(shows("Regenerating..."));
        assertTrue(shows("[Rich]\n0x Oak Tree"));
        assertTrue(shows("Oak Tree\nRespawns in 30s"));
    }

    @Test void trackedLiveLabelIsNotDepleted() {
        assertFalse(shows("\n10x Oak Tree\n1 Woodcutting Power"));
        assertFalse(shows("[Weak]\n3x Oak Tree\n5 Woodcutting Power"));
    }
}
