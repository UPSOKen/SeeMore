package com.froobworld.seemore.controller;

public record PlayerDistanceStatus(String playerName, String worldName, String profileName, DistanceMode mode,
                                   int clientViewDistance, int configuredMaximum, int worldViewDistance,
                                   int targetViewDistance, int targetSendDistance, int liveViewDistance,
                                   int liveSendDistance, int simulationDistance, int afkMaximum,
                                   int afkMinimumReduction, int undergroundMaximum,
                                   boolean naturalCeilingCheckEnabled,
                                   int naturalCeilingSearchDistance, int minimumNaturalCeilingThickness,
                                   boolean undergroundBypassPermissionEnabled,
                                   boolean undergroundBypassPermissionGranted) {
}
