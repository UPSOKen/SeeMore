package com.froobworld.seemore.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewDistancePolicyTest {
    @Test
    void appliesTheConfiguredAndClientCeilings() {
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(20, 12, 16, 8,
                false, 10, 3, false, 8);

        assertEquals(12, result.viewDistance());
        assertEquals(12, result.sendDistance());
    }

    @Test
    void resolvesNegativeOneToTheCurrentWorldViewDistance() {
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(24, -1, 14, 8,
                false, 10, 3, false, 8);

        assertEquals(14, result.viewDistance());
        assertEquals(14, result.sendDistance());
    }

    @Test
    void appliesAfkAsACapWithoutGoingBelowSimulationDistance() {
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(18, 18, 20, 10,
                true, 8, 3, false, 8);

        assertEquals(10, result.viewDistance());
        assertEquals(10, result.sendDistance());
    }

    @Test
    void neverPassesAnOutOfRangeSendDistanceToPaper() {
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(32, 32, 32, 10,
                false, 10, 3, false, 8);

        assertEquals(32, result.viewDistance());
        assertEquals(32, result.sendDistance());
    }

    @Test
    void appliesUndergroundAsACapForAnActivePlayer() {
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(20, 18, 20, 6,
                false, 10, 3, true, 8);

        assertEquals(8, result.viewDistance());
        assertEquals(8, result.sendDistance());
    }

    @Test
    void afkTakesPrecedenceOverTheUndergroundCap() {
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(20, 20, 20, 4,
                true, 8, 3, true, 6);

        assertEquals(8, result.viewDistance());
        assertEquals(8, result.sendDistance());
    }

    @Test
    void skipsAfkReductionWhenItWouldSaveFewerThanTheConfiguredMinimum() {
        ViewDistancePolicy.Result result = ViewDistancePolicy.calculate(12, 12, 20, 8,
                true, 10, 3, true, 6);

        assertEquals(12, result.viewDistance());
        assertEquals(12, result.sendDistance());
    }
}
