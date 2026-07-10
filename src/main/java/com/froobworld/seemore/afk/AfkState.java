package com.froobworld.seemore.afk;

import java.time.Duration;

public final class AfkState {
    private long lastActivityNanos;
    private long firstLookEventNanos;
    private int lookEventCount;
    private boolean afk;

    public AfkState(long nowNanos) {
        this.lastActivityNanos = nowNanos;
    }

    public synchronized Transition checkTimeout(long nowNanos, Duration timeout) {
        if (!afk && nowNanos - lastActivityNanos >= timeout.toNanos()) {
            afk = true;
            clearLookEvents();
            return Transition.BECAME_AFK;
        }
        return Transition.NONE;
    }

    public synchronized Transition recordDefiniteActivity(long nowNanos) {
        boolean wasAfk = afk;
        afk = false;
        lastActivityNanos = nowNanos;
        clearLookEvents();
        return wasAfk ? Transition.BECAME_ACTIVE : Transition.NONE;
    }

    public synchronized Transition recordLookActivity(double change, long nowNanos, double minimumChange,
                                                      int requiredEvents, Duration eventWindow) {
        if (change < minimumChange) {
            return Transition.NONE;
        }

        if (lookEventCount == 0 || nowNanos - firstLookEventNanos > eventWindow.toNanos()) {
            firstLookEventNanos = nowNanos;
            lookEventCount = 1;
        } else {
            lookEventCount++;
        }

        if (lookEventCount >= requiredEvents) {
            return recordDefiniteActivity(nowNanos);
        }
        return Transition.NONE;
    }

    public synchronized boolean isAfk() {
        return afk;
    }

    private void clearLookEvents() {
        firstLookEventNanos = 0;
        lookEventCount = 0;
    }

    public enum Transition {
        NONE,
        BECAME_AFK,
        BECAME_ACTIVE
    }
}
