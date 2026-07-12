package com.froobworld.seemore.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewDistanceControllerTest {
    @Test
    void normalBackgroundChecksRunOnlyWhileActive() {
        assertTrue(ViewDistanceController.shouldRunNormalChecks(false));
        assertFalse(ViewDistanceController.shouldRunNormalChecks(true));
    }

    @Test
    void cachedProfilesAreReusedForOrdinaryDistanceRecalculations() {
        assertFalse(ViewDistanceController.shouldResolveProfile(false, "donor", 16));
    }

    @Test
    void profilesAreResolvedForLifecycleRefreshesOrMissingCacheEntries() {
        assertTrue(ViewDistanceController.shouldResolveProfile(true, "donor", 16));
        assertTrue(ViewDistanceController.shouldResolveProfile(false, null, null));
    }

    @Test
    void caveBypassPermissionIsCheckedOnlyDuringEnabledLifecycleRefreshes() {
        assertTrue(ViewDistanceController.shouldCheckUndergroundBypass(true, true));
        assertFalse(ViewDistanceController.shouldCheckUndergroundBypass(false, true));
        assertFalse(ViewDistanceController.shouldCheckUndergroundBypass(true, false));
    }
}
