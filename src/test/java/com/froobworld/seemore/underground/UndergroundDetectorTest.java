package com.froobworld.seemore.underground;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UndergroundDetectorTest {
    @Test
    void detectsPlayersAtOrBelowTheMinimumTerrainDepth() {
        assertTrue(UndergroundDetector.isCandidate(true, 80, 70, 10));
        assertFalse(UndergroundDetector.isCandidate(true, 80, 71, 10));
    }

    @Test
    void treatsEligibleWorldsWithoutSkylightAsUnderground() {
        assertTrue(UndergroundDetector.isCandidate(false, 0, 100, 10));
    }

    @Test
    void usesASeparateExitDepthAfterUndergroundModeActivates() {
        assertEquals(10, UndergroundDetector.requiredDepth(false, 10, 5));
        assertEquals(5, UndergroundDetector.requiredDepth(true, 10, 5));
    }

    @Test
    void bypassPermissionMakesAnOtherwiseEligiblePlayerIneligible() {
        assertFalse(UndergroundDetector.isBypassed(false, true));
        assertTrue(UndergroundDetector.isBypassed(true, true));
        assertFalse(UndergroundDetector.isBypassed(true, false));
    }
}
