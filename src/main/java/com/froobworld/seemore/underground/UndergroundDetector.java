package com.froobworld.seemore.underground;

public final class UndergroundDetector {
    private UndergroundDetector() {
    }

    public static boolean isCandidate(boolean hasSkyLight, int surfaceY, int eyeY, int minimumDepth) {
        return !hasSkyLight || surfaceY - eyeY >= minimumDepth;
    }

    public static int requiredDepth(boolean underground, int entryDepth, int exitDepth) {
        return underground ? exitDepth : entryDepth;
    }

    public static boolean isEligible(boolean enabled, boolean worldEnabled, boolean afk, boolean bypass) {
        return enabled && worldEnabled && !afk && !bypass;
    }

    public static boolean isBypassed(boolean bypassPermissionEnabled, boolean hasBypassPermission) {
        return bypassPermissionEnabled && hasBypassPermission;
    }

    public static boolean hasRequiredEvidence(boolean depthEvidence, boolean naturalCeilingCheckEnabled,
                                              boolean naturalCeilingEvidence) {
        return depthEvidence && (!naturalCeilingCheckEnabled || naturalCeilingEvidence);
    }
}
