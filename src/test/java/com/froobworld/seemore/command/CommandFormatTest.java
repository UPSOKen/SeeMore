package com.froobworld.seemore.command;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandFormatTest {
    @Test
    void usesDistinctColorsForHeadingsLabelsAndValues() {
        assertEquals(NamedTextColor.GOLD, CommandFormat.header("Heading").color());
        assertEquals(NamedTextColor.GRAY, CommandFormat.line("Label", CommandFormat.value(8)).color());
        assertEquals(NamedTextColor.AQUA, CommandFormat.value(8).color());
    }
}
