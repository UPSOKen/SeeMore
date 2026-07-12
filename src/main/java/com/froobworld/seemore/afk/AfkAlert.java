package com.froobworld.seemore.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

final class AfkAlert {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private AfkAlert() {
    }

    static Component parse(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return MINI_MESSAGE.deserialize(message);
    }
}
