package com.froobworld.seemore.underground;

import java.time.Duration;

public final class UndergroundState {
    private static final long NOT_TRACKING = Long.MIN_VALUE;
    private long undergroundEvidenceSince = NOT_TRACKING;
    private long surfaceEvidenceSince = NOT_TRACKING;
    private boolean underground;

    public synchronized Transition update(boolean undergroundEvidence, long nowNanos,
                                          Duration enterAfter, Duration exitAfter) {
        if (undergroundEvidence) {
            surfaceEvidenceSince = NOT_TRACKING;
            if (underground) {
                return Transition.NONE;
            }
            if (undergroundEvidenceSince == NOT_TRACKING) {
                undergroundEvidenceSince = nowNanos;
                return Transition.NONE;
            }
            if (nowNanos - undergroundEvidenceSince >= enterAfter.toNanos()) {
                underground = true;
                undergroundEvidenceSince = NOT_TRACKING;
                return Transition.BECAME_UNDERGROUND;
            }
            return Transition.NONE;
        }

        undergroundEvidenceSince = NOT_TRACKING;
        if (!underground) {
            return Transition.NONE;
        }
        if (surfaceEvidenceSince == NOT_TRACKING) {
            surfaceEvidenceSince = nowNanos;
            return Transition.NONE;
        }
        if (nowNanos - surfaceEvidenceSince >= exitAfter.toNanos()) {
            underground = false;
            surfaceEvidenceSince = NOT_TRACKING;
            return Transition.BECAME_SURFACE;
        }
        return Transition.NONE;
    }

    public synchronized Transition reset() {
        boolean wasUnderground = underground;
        underground = false;
        undergroundEvidenceSince = NOT_TRACKING;
        surfaceEvidenceSince = NOT_TRACKING;
        return wasUnderground ? Transition.BECAME_SURFACE : Transition.NONE;
    }

    public synchronized boolean isUnderground() {
        return underground;
    }

    public enum Transition {
        NONE,
        BECAME_UNDERGROUND,
        BECAME_SURFACE
    }
}
