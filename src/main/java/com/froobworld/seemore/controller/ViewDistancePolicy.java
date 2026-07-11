package com.froobworld.seemore.controller;

public final class ViewDistancePolicy {
    private static final int MAXIMUM_PAPER_DISTANCE = 32;
    private static final int MINIMUM_SEND_DISTANCE = 2;

    private ViewDistancePolicy() {
    }

    public static Result calculate(int clientViewDistance, int configuredMaximum, int worldViewDistance,
                                   int simulationDistance, boolean afk, int afkMaximum,
                                   boolean underground, int undergroundMaximum) {
        int ceiling = configuredMaximum < 0 ? worldViewDistance : configuredMaximum;
        ceiling = Math.min(ceiling, MAXIMUM_PAPER_DISTANCE);
        if (afk) {
            ceiling = Math.min(ceiling, afkMaximum);
        } else if (underground) {
            ceiling = Math.min(ceiling, undergroundMaximum);
        }

        int requestedDistance = Math.min(ceiling, clientViewDistance);
        int viewDistance = Math.max(simulationDistance, requestedDistance);
        int sendDistance = Math.min(MAXIMUM_PAPER_DISTANCE,
                Math.max(MINIMUM_SEND_DISTANCE, requestedDistance) + 1);
        return new Result(viewDistance, sendDistance);
    }

    public record Result(int viewDistance, int sendDistance) {
    }
}
