package com.froobworld.seemore.afk;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AfkStateTest {
    private static final Duration TIMEOUT = Duration.ofMinutes(10);
    private static final Duration LOOK_WINDOW = Duration.ofSeconds(2);

    @Test
    void becomesAfkAfterTheConfiguredTimeout() {
        AfkState state = new AfkState(0);

        assertEquals(AfkState.Transition.NONE, state.checkTimeout(TIMEOUT.minusSeconds(1).toNanos(), TIMEOUT));
        assertEquals(AfkState.Transition.BECAME_AFK, state.checkTimeout(TIMEOUT.toNanos(), TIMEOUT));
        assertTrue(state.isAfk());
    }

    @Test
    void definiteInputWakesImmediately() {
        AfkState state = new AfkState(0);
        state.checkTimeout(TIMEOUT.toNanos(), TIMEOUT);

        assertEquals(AfkState.Transition.BECAME_ACTIVE, state.recordDefiniteActivity(TIMEOUT.plusSeconds(1).toNanos()));
        assertFalse(state.isAfk());
    }

    @Test
    void oneSmallOrQualifyingLookEventDoesNotWakeButTwoDo() {
        AfkState state = new AfkState(0);
        state.checkTimeout(TIMEOUT.toNanos(), TIMEOUT);
        long firstLook = TIMEOUT.plusSeconds(1).toNanos();

        assertEquals(AfkState.Transition.NONE, state.recordLookActivity(1.9, firstLook, 2.0, 2, LOOK_WINDOW));
        assertEquals(AfkState.Transition.NONE, state.recordLookActivity(3.0, firstLook, 2.0, 2, LOOK_WINDOW));
        assertEquals(AfkState.Transition.BECAME_ACTIVE,
                state.recordLookActivity(2.5, firstLook + Duration.ofMillis(100).toNanos(), 2.0, 2, LOOK_WINDOW));
    }

    @Test
    void lookConfirmationsOutsideTheWindowDoNotAccumulate() {
        AfkState state = new AfkState(0);
        state.checkTimeout(TIMEOUT.toNanos(), TIMEOUT);
        long firstLook = TIMEOUT.plusSeconds(1).toNanos();

        state.recordLookActivity(3.0, firstLook, 2.0, 2, LOOK_WINDOW);
        assertEquals(AfkState.Transition.NONE,
                state.recordLookActivity(3.0, firstLook + LOOK_WINDOW.plusMillis(1).toNanos(), 2.0, 2, LOOK_WINDOW));
        assertTrue(state.isAfk());
    }

    @Test
    void confirmedCameraActivityResetsTheActiveTimeout() {
        AfkState state = new AfkState(0);
        long activityTime = Duration.ofMinutes(9).toNanos();

        state.recordLookActivity(3.0, activityTime, 2.0, 2, LOOK_WINDOW);
        state.recordLookActivity(3.0, activityTime + 1, 2.0, 2, LOOK_WINDOW);

        assertEquals(AfkState.Transition.NONE, state.checkTimeout(Duration.ofMinutes(18).toNanos(), TIMEOUT));
    }
}
