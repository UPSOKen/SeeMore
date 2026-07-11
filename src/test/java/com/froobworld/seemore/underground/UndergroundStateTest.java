package com.froobworld.seemore.underground;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class UndergroundStateTest {
    private static final Duration ENTER_AFTER = Duration.ofMinutes(2);
    private static final Duration EXIT_AFTER = Duration.ofSeconds(5);

    @Test
    void entersOnlyAfterContinuousUndergroundEvidence() {
        UndergroundState state = new UndergroundState();

        assertEquals(UndergroundState.Transition.NONE,
                state.update(true, 0, ENTER_AFTER, EXIT_AFTER));
        assertEquals(UndergroundState.Transition.NONE,
                state.update(true, ENTER_AFTER.minusMillis(1).toNanos(), ENTER_AFTER, EXIT_AFTER));
        assertEquals(UndergroundState.Transition.BECAME_UNDERGROUND,
                state.update(true, ENTER_AFTER.toNanos(), ENTER_AFTER, EXIT_AFTER));
        assertTrue(state.isUnderground());
    }

    @Test
    void interruptedEvidenceRestartsTheEntryTimer() {
        UndergroundState state = new UndergroundState();

        state.update(true, 0, ENTER_AFTER, EXIT_AFTER);
        state.update(false, Duration.ofMinutes(1).toNanos(), ENTER_AFTER, EXIT_AFTER);
        state.update(true, Duration.ofMinutes(2).toNanos(), ENTER_AFTER, EXIT_AFTER);

        assertEquals(UndergroundState.Transition.NONE,
                state.update(true, Duration.ofMinutes(3).toNanos(), ENTER_AFTER, EXIT_AFTER));
        assertFalse(state.isUnderground());
    }

    @Test
    void exitsOnlyAfterContinuousSurfaceEvidence() {
        UndergroundState state = undergroundState();
        long surfaceStart = ENTER_AFTER.plusSeconds(1).toNanos();

        assertEquals(UndergroundState.Transition.NONE,
                state.update(false, surfaceStart, ENTER_AFTER, EXIT_AFTER));
        assertEquals(UndergroundState.Transition.NONE,
                state.update(false, surfaceStart + EXIT_AFTER.minusMillis(1).toNanos(), ENTER_AFTER, EXIT_AFTER));
        assertEquals(UndergroundState.Transition.BECAME_SURFACE,
                state.update(false, surfaceStart + EXIT_AFTER.toNanos(), ENTER_AFTER, EXIT_AFTER));
        assertFalse(state.isUnderground());
    }

    @Test
    void resetImmediatelyClearsUndergroundStateAndTimers() {
        UndergroundState state = undergroundState();

        assertEquals(UndergroundState.Transition.BECAME_SURFACE, state.reset());
        assertFalse(state.isUnderground());
        assertEquals(UndergroundState.Transition.NONE,
                state.update(true, ENTER_AFTER.plusSeconds(1).toNanos(), ENTER_AFTER, EXIT_AFTER));
    }

    private static UndergroundState undergroundState() {
        UndergroundState state = new UndergroundState();
        state.update(true, 0, ENTER_AFTER, EXIT_AFTER);
        state.update(true, ENTER_AFTER.toNanos(), ENTER_AFTER, EXIT_AFTER);
        return state;
    }
}
