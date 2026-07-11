package com.froobworld.seemore.config;

public enum DistanceResolutionSource {
    GROUP_WORLD_OVERRIDE("group world override"),
    WORLD_SETTING("world setting"),
    GROUP_DEFAULT_OVERRIDE("group default override"),
    DEFAULT_SETTING("default setting");

    private final String description;

    DistanceResolutionSource(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
