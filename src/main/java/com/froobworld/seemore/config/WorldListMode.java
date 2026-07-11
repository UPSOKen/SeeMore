package com.froobworld.seemore.config;

import java.util.Locale;

public enum WorldListMode {
    WHITELIST,
    BLACKLIST;

    static WorldListMode parse(String value, String path) {
        if (value == null) {
            throw new IllegalArgumentException(path + " is required.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(path + " must be whitelist or blacklist.", exception);
        }
    }
}
