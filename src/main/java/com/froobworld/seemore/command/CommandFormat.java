package com.froobworld.seemore.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static net.kyori.adventure.text.Component.text;

final class CommandFormat {
    private CommandFormat() {
    }

    static Component header(String value) {
        return text(value, NamedTextColor.GOLD);
    }

    static Component line(String label, Component value) {
        return text(label + ": ", NamedTextColor.GRAY).append(value);
    }

    static Component value(String value) {
        return text(value, NamedTextColor.AQUA);
    }

    static Component value(int value) {
        return value(Integer.toString(value));
    }

    static Component configuredDistance(int configuredMaximum, int serverViewDistance) {
        if (configuredMaximum < 0) {
            return value("Server default (" + serverViewDistance + ")");
        }
        return value(configuredMaximum);
    }
}
