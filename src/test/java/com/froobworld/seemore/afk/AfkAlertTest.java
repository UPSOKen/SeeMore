package com.froobworld.seemore.afk;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AfkAlertTest {
    @Test
    void blankMessagesDoNotCreateAnAlert() {
        assertNull(AfkAlert.parse(""));
        assertNull(AfkAlert.parse("   "));
    }

    @Test
    void parsesConfiguredMessagesAsMiniMessageComponents() {
        Component alert = AfkAlert.parse("<yellow>You are AFK</yellow>");

        assertEquals("You are AFK", PlainTextComponentSerializer.plainText().serialize(alert));
        assertEquals(NamedTextColor.YELLOW, alert.color());
    }
}
