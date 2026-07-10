package com.froobworld.seemore.config;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern DURATION = Pattern.compile("^(\\d+)\\s*(ms|t|s|m|h|d)$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Duration parse(String value, String path) {
        if (value == null) {
            throw new IllegalArgumentException(path + " is required.");
        }
        Matcher matcher = DURATION.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(path + " must be a positive duration such as 10s, 10m, or 1h.");
        }

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(path + " is too large.", exception);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException(path + " must be greater than zero.");
        }

        try {
            return switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "ms" -> Duration.ofMillis(amount);
                case "t" -> Duration.ofMillis(Math.multiplyExact(amount, 50));
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> throw new IllegalStateException("Unhandled duration unit.");
            };
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(path + " is too large.", exception);
        }
    }

    public static long toTicks(Duration duration) {
        long milliseconds = duration.toMillis();
        return Math.max(1, Math.addExact(milliseconds, 49) / 50);
    }
}
