package com.froobworld.seemore.afk;

import org.bukkit.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfkTrackerTest {
    @Test
    void treatsHeldMovementInputAsActivity() {
        assertTrue(AfkTracker.hasMovementInput(new TestInput(true, false, false, false, false, false, false)));
    }

    @Test
    void doesNotTreatAnEmptyInputStateAsActivity() {
        assertFalse(AfkTracker.hasMovementInput(new TestInput(false, false, false, false, false, false, false)));
    }

    private record TestInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump,
                             boolean sneak, boolean sprint) implements Input {
        @Override
        public boolean isForward() {
            return forward;
        }

        @Override
        public boolean isBackward() {
            return backward;
        }

        @Override
        public boolean isLeft() {
            return left;
        }

        @Override
        public boolean isRight() {
            return right;
        }

        @Override
        public boolean isJump() {
            return jump;
        }

        @Override
        public boolean isSneak() {
            return sneak;
        }

        @Override
        public boolean isSprint() {
            return sprint;
        }
    }
}
