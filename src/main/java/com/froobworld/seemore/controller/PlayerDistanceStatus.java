package com.froobworld.seemore.controller;

public record PlayerDistanceStatus(String playerName, String worldName, String profileName, DistanceMode mode,
                                   int clientViewDistance, int configuredMaximum, int worldViewDistance,
                                   int targetViewDistance, int targetSendDistance, int liveViewDistance,
                                   int liveSendDistance, int simulationDistance, int afkMaximum,
                                   int undergroundMaximum, boolean undergroundBypassPermissionEnabled,
                                   boolean undergroundBypassPermissionGranted) {
}
