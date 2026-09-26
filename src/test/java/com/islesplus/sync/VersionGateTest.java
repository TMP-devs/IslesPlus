package com.islesplus.sync;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionGateTest {
    private static boolean kills(String ruleJson, String version) {
        return VersionGate.matches(JsonParser.parseString(ruleJson), version);
    }

    @Test void booleansIgnoreTheVersion() {
        assertTrue(kills("true", "1.0.1"));
        assertTrue(kills("true", ""));
        assertFalse(kills("false", "1.0.1"));
    }

    @Test void plainVersionIsExact() {
        assertTrue(kills("\"1.0.2\"", "1.0.2"));
        assertFalse(kills("\"1.0.2\"", "1.0.3"));
        assertFalse(kills("\"1.0.2\"", "1.0.1"));
        assertTrue(kills("\"=1.0.2\"", "1.0.2"));
    }

    @Test void missingPartsCountAsZero() {
        assertTrue(kills("\"1.0\"", "1.0.0"));
        assertTrue(kills("\"1.1.0\"", "1.1"));
    }

    @Test void comparisons() {
        assertTrue(kills("\"<1.0.3\"", "1.0.2"));
        assertFalse(kills("\"<1.0.3\"", "1.0.3"));
        assertTrue(kills("\"<=1.0.3\"", "1.0.3"));
        assertTrue(kills("\">1.0.3\"", "1.0.10"));
        assertFalse(kills("\">1.0.3\"", "1.0.3"));
        assertTrue(kills("\">=1.0.3\"", "1.0.3"));
    }

    @Test void numericNotAlphabetical() {
        assertTrue(kills("\"<1.0.10\"", "1.0.9"));
    }

    @Test void earlierPartsOutrankLaterOnes() {
        assertTrue(kills("\">1.0.3\"", "1.1.1"));
        assertFalse(kills("\"<1.0.3\"", "1.1.1"));
        assertTrue(kills("\">1.9.9\"", "2.0.0"));
        assertTrue(kills("\"<1.1.0\"", "1.0.99"));
    }

    @Test void spaceSeparatedIsARange() {
        String range = "\">=1.0.2 <1.0.5\"";
        assertFalse(kills(range, "1.0.1"));
        assertTrue(kills(range, "1.0.2"));
        assertTrue(kills(range, "1.0.4"));
        assertFalse(kills(range, "1.0.5"));
    }

    @Test void listMatchesAny() {
        String list = "[\"1.0.2\", \">=1.1.0\"]";
        assertTrue(kills(list, "1.0.2"));
        assertFalse(kills(list, "1.0.3"));
        assertTrue(kills(list, "1.2.0"));
        assertFalse(kills("[]", "1.0.2"));
    }

    @Test void starMatchesEverythingEvenUnknown() {
        assertTrue(kills("\"*\"", "1.0.2"));
        assertTrue(kills("\"*\"", ""));
    }

    @Test void suffixesAreIgnored() {
        assertTrue(kills("\"1.0.2\"", "1.0.2-beta.1"));
        assertTrue(kills("\"1.0.2\"", "1.0.2+1.21.11"));
        assertTrue(kills("\"v1.0.2\"", "1.0.2"));
    }

    @Test void unknownVersionOnlyMatchesUnconditional() {
        assertFalse(kills("\"<9.0.0\"", ""));
        assertFalse(kills("\"1.0.2\"", "dev"));
    }

    @Test void malformedRulesFailOpen() {
        assertFalse(kills("\"\"", "1.0.2"));
        assertFalse(kills("\"abc\"", "1.0.2"));
        assertFalse(kills("\"<\"", "1.0.2"));
        assertFalse(kills("\"1..2\"", "1.0.2"));
        assertFalse(kills("{}", "1.0.2"));
        assertFalse(kills("null", "1.0.2"));
        assertFalse(kills("[{}, 3]", "1.0.2"));
    }

    @Test void updateOnlyWhenPublishedIsNewer() {
        org.junit.jupiter.api.Assertions.assertTrue(VersionGate.isNewer("1.0.2", "1.0.1"));
        org.junit.jupiter.api.Assertions.assertFalse(VersionGate.isNewer("1.0.1", "1.0.2"));
        org.junit.jupiter.api.Assertions.assertFalse(VersionGate.isNewer("1.0.2", "1.0.2"));
        org.junit.jupiter.api.Assertions.assertTrue(VersionGate.isNewer("1.1", "1.0.9"));
        org.junit.jupiter.api.Assertions.assertFalse(VersionGate.isNewer("garbage", "1.0.2"));
    }
}
