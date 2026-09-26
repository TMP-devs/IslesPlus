package com.islesplus.sync;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureStateTest {
    private static FeatureState parse(String json, String version) {
        return FeatureState.parse(JsonParser.parseString(json), version);
    }

    private static FeatureState state(boolean disabled, String disabledReason, boolean killed) {
        return new FeatureState(disabled, disabledReason, killed, null, null);
    }

    private static String tooltip(String json, String version) {
        return parse(json, version).tooltip();
    }

    // --- global fields ---

    @Test void emptyOrAllFalseIsNormal() {
        assertEquals(FeatureState.NORMAL, parse("{}", "1.0.2"));
        assertEquals(FeatureState.NORMAL, parse("{\"disabled\": false, \"killed\": false}", "1.0.2"));
        assertFalse(parse("{}", "1.0.2").blocked());
    }

    @Test void globalDisabledWithReason() {
        FeatureState s = parse("{\"disabled\": true, \"disabled_reason\": \"Server changed\"}", "1.0.2");
        assertEquals(state(true, "Server changed", false), s);
        assertTrue(s.blocked());
    }

    @Test void globalKilled() {
        assertEquals(state(false, "", true), parse("{\"killed\": true}", "1.0.2"));
    }

    @Test void oldKilledReasonFieldIsIgnored() {
        assertEquals(state(false, "", true), parse("{\"killed\": true, \"killed_reason\": \"crashes\"}", "1.0.2"));
        assertFalse(parse("{\"killed_reason\": \"x\"}", "1.0.2").blocked());
    }

    @Test void globalAppliesOnUnknownVersion() {
        assertTrue(parse("{\"killed\": true}", "").killed());
    }

    @Test void reasonAloneDoesNotBlock() {
        assertFalse(parse("{\"disabled_reason\": \"x\"}", "1.0.2").blocked());
    }

    // --- versions ---

    @Test void versionEntryOnlyAppliesToItsVersions() {
        String json = "{\"versions\": {"
            + "\"1.0.2\": {\"killed\": true},"
            + "\">=1.0.3 <1.0.5\": {\"disabled\": true, \"disabled_reason\": \"Update to 1.0.5\"}}}";
        assertEquals(FeatureState.NORMAL, parse(json, "1.0.1"));
        assertEquals(state(false, "", true), parse(json, "1.0.2"));
        assertEquals(state(true, "Update to 1.0.5", false), parse(json, "1.0.3"));
        assertEquals(state(true, "Update to 1.0.5", false), parse(json, "1.0.4"));
        assertEquals(FeatureState.NORMAL, parse(json, "1.0.5"));
        assertEquals(FeatureState.NORMAL, parse(json, ""));   // unknown version: no key matches
    }

    @Test void commaKeyMatchesAny() {
        String json = "{\"versions\": {\"1.0.2, 1.0.4\": {\"disabled\": true}}}";
        assertTrue(parse(json, "1.0.2").disabled());
        assertFalse(parse(json, "1.0.3").disabled());
        assertTrue(parse(json, "1.0.4").disabled());
    }

    @Test void starKeyMatchesEverything() {
        assertTrue(parse("{\"versions\": {\"*\": {\"disabled\": true}}}", "").disabled());
        assertTrue(parse("{\"versions\": {\"*\": {\"disabled\": true}}}", "1.0.9").disabled());
    }

    @Test void disabledAndKilledOnSameVersionBothSet() {
        FeatureState s = parse("{\"disabled\": true, \"versions\": {\"1.0.2\": {\"killed\": true}}}", "1.0.2");
        assertTrue(s.disabled());
        assertTrue(s.killed());   // killed wins in the UI; both stop the code
    }

    // --- global always beats versions ---

    @Test void versionFalseCannotUndoGlobalDisabled() {
        String json = "{\"disabled\": true, \"disabled_reason\": \"global\","
            + " \"versions\": {\"1.0.2\": {\"disabled\": false, \"disabled_reason\": \"version\"}}}";
        assertEquals(state(true, "global", false), parse(json, "1.0.2"));
    }

    @Test void versionFalseCannotUndoGlobalKilled() {
        String json = "{\"killed\": true, \"versions\": {\"1.0.2\": {\"killed\": false}, \"*\": {\"killed\": false}}}";
        assertTrue(parse(json, "1.0.2").killed());
        assertTrue(parse(json, "1.0.3").killed());
    }

    @Test void globalReasonBeatsVersionReason() {
        String json = "{\"disabled\": true, \"disabled_reason\": \"global\","
            + " \"versions\": {\"1.0.2\": {\"disabled\": true, \"disabled_reason\": \"version\"}}}";
        assertEquals("global", parse(json, "1.0.2").disabledReason());
    }

    @Test void versionReasonFillsInWhenGlobalHasNone() {
        String json = "{\"disabled\": true, \"versions\": {\"1.0.2\": {\"disabled_reason\": \"version\"}}}";
        assertEquals(state(true, "version", false), parse(json, "1.0.2"));
        assertEquals(state(true, "", false), parse(json, "1.0.3"));
    }

    @Test void entrySayingFalseDoesNotSupplyReason() {
        String json = "{\"disabled\": true, \"versions\": {\"1.0.2\": {\"disabled\": false, \"disabled_reason\": \"nope\"}}}";
        assertEquals("", parse(json, "1.0.2").disabledReason());
    }

    @Test void firstMatchingVersionReasonWins() {
        String json = "{\"versions\": {"
            + "\"<1.0.5\": {\"disabled\": true, \"disabled_reason\": \"first\"},"
            + "\"1.0.2\": {\"disabled\": true, \"disabled_reason\": \"second\"}}}";
        assertEquals("first", parse(json, "1.0.2").disabledReason());
    }

    // --- mistakes fail open ---

    @Test void wrongTypesAreIgnored() {
        assertEquals(FeatureState.NORMAL, parse("{\"disabled\": \"yes\", \"killed\": 1}", "1.0.2"));
        assertEquals(state(true, "", false), parse("{\"disabled\": \"TRUE\", \"disabled_reason\": 5}", "1.0.2"));
        assertEquals(FeatureState.NORMAL, parse("true", "1.0.2"));
        assertEquals(FeatureState.NORMAL, parse("null", "1.0.2"));
        assertEquals(FeatureState.NORMAL, parse("{\"versions\": {\"1.0.2\": true}}", "1.0.2"));
        assertEquals(FeatureState.NORMAL, parse("{\"versions\": [\"1.0.2\"]}", "1.0.2"));
    }

    @Test void badVersionKeyMatchesNothing() {
        assertEquals(FeatureState.NORMAL, parse("{\"versions\": {\"banana\": {\"killed\": true}}}", "1.0.2"));
        assertEquals(FeatureState.NORMAL, parse("{\"versions\": {\"\": {\"killed\": true}}}", "1.0.2"));
        assertEquals(FeatureState.NORMAL, parse("{\"versions\": {\" , \": {\"killed\": true}}}", "1.0.2"));
    }

    // --- tooltips ---

    @Test void noTooltipFieldMeansNotSet() {
        assertNull(tooltip("{}", "1.0.2"));
        assertNull(tooltip("{\"disabled\": true}", "1.0.2"));
    }

    @Test void globalTooltip() {
        assertEquals("Shaders may break this", tooltip("{\"tooltip\": \"  Shaders may break this \"}", "1.0.2"));
        assertEquals("", tooltip("{\"tooltip\": \"\"}", "1.0.2"));
    }

    @Test void versionTooltipReplacesGlobalOnItsVersionOnly() {
        String json = "{\"tooltip\": \"global\", \"versions\": {\"1.0.1\": {\"tooltip\": \"old version\"}}}";
        assertEquals("old version", tooltip(json, "1.0.1"));
        assertEquals("global", tooltip(json, "1.0.2"));
    }

    @Test void versionEmptyTooltipHidesOnThatVersion() {
        String json = "{\"tooltip\": \"global\", \"versions\": {\"1.0.1\": {\"tooltip\": \"\"}}}";
        assertEquals("", tooltip(json, "1.0.1"));
        assertEquals("global", tooltip(json, "1.0.2"));
    }

    @Test void versionTooltipWithoutGlobal() {
        String json = "{\"versions\": {\"<1.0.4\": {\"tooltip\": \"update\"}}}";
        assertEquals("update", tooltip(json, "1.0.3"));
        assertNull(tooltip(json, "1.0.4"));
    }

    @Test void firstMatchingVersionTooltipWins() {
        String json = "{\"versions\": {\"<1.0.5\": {\"tooltip\": \"first\"}, \"1.0.2\": {\"tooltip\": \"second\"}}}";
        assertEquals("first", tooltip(json, "1.0.2"));
    }

    @Test void versionEntryWithoutTooltipKeepsGlobal() {
        String json = "{\"tooltip\": \"global\", \"versions\": {\"1.0.2\": {\"disabled\": true}}}";
        assertEquals("global", tooltip(json, "1.0.2"));
    }

    @Test void nonStringTooltipIsIgnored() {
        assertNull(tooltip("{\"tooltip\": 5}", "1.0.2"));
        assertEquals("global", tooltip("{\"tooltip\": \"global\", \"versions\": {\"1.0.2\": {\"tooltip\": false}}}", "1.0.2"));
    }

    @Test void tooltipAloneDoesNotBlock() {
        assertFalse(parse("{\"tooltip\": \"hi\"}", "1.0.2").blocked());
    }

    @Test void resolveTooltip() {
        assertEquals("remote", FeatureState.resolveTooltip("remote", "built-in"));
        assertEquals("remote", FeatureState.resolveTooltip("remote", null));
        assertNull(FeatureState.resolveTooltip("", "built-in"));
        assertEquals("built-in", FeatureState.resolveTooltip(null, "built-in"));
        assertNull(FeatureState.resolveTooltip(null, null));
    }

    // --- legacy "killed" object ---

    @Test void legacyRuleMeansDisabled() {
        assertEquals(state(true, "", false), FeatureState.legacy(JsonParser.parseString("true"), "1.0.2"));
        assertEquals(state(true, "", false), FeatureState.legacy(JsonParser.parseString("\"<1.0.4\""), "1.0.2"));
        assertEquals(FeatureState.NORMAL, FeatureState.legacy(JsonParser.parseString("false"), "1.0.2"));
        assertEquals(FeatureState.NORMAL, FeatureState.legacy(JsonParser.parseString("\"<1.0.4\""), "1.0.4"));
    }

    private static Boolean beta(String json, String version) { return parse(json, version).beta(); }

    @Test
    void betaUnsetIsNull() {
        assertNull(beta("{\"disabled\": false}", "1.0.2"));
    }

    @Test
    void betaGlobal() {
        assertEquals(Boolean.TRUE, beta("{\"beta\": true}", "1.0.2"));
        assertEquals(Boolean.FALSE, beta("{\"beta\": \"false\"}", "1.0.2"));
    }

    @Test
    void betaVersionReplacesGlobal() {
        String json = "{\"beta\": true, \"versions\": {\">=1.0.3\": {\"beta\": false}, \"1.0.2\": {\"tooltip\": \"x\"}}}";
        assertEquals(Boolean.FALSE, beta(json, "1.0.3"));
        assertEquals(Boolean.TRUE, beta(json, "1.0.2"));   // matching entry without "beta" keeps the global
        assertEquals(Boolean.TRUE, beta(json, "1.0.1"));
    }

    @Test
    void betaOnlyInAVersion() {
        String json = "{\"versions\": {\"1.0.2\": {\"beta\": true}}}";
        assertEquals(Boolean.TRUE, beta(json, "1.0.2"));
        assertNull(beta(json, "1.0.1"));
    }

    @Test
    void betaDoesNotBlock() {
        assertFalse(parse("{\"beta\": true}", "1.0.2").blocked());
    }
}
