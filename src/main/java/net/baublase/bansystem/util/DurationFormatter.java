package net.baublase.bansystem.util;

import net.baublase.bansystem.i18n.Message;
import net.baublase.bansystem.i18n.MessageService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DurationFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private DurationFormatter() {
    }

    public static String format(Duration duration, MessageService messages, Locale locale) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return messages.plain(locale, Message.DURATION_PERMANENT);
        }
        List<String> parts = new ArrayList<>();
        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);
        long seconds = duration.toSeconds();
        if (days > 0) {
            parts.add(messages.plain(locale, Message.DURATION_DAYS, "value", String.valueOf(days)));
        }
        if (hours > 0) {
            parts.add(messages.plain(locale, Message.DURATION_HOURS, "value", String.valueOf(hours)));
        }
        if (minutes > 0) {
            parts.add(messages.plain(locale, Message.DURATION_MINUTES, "value", String.valueOf(minutes)));
        }
        if (seconds > 0 && parts.isEmpty()) {
            parts.add(messages.plain(locale, Message.DURATION_SECONDS, "value", String.valueOf(seconds)));
        }
        return String.join(" ", parts);
    }

    public static String remaining(Instant expiresAt, MessageService messages, Locale locale) {
        if (expiresAt == null) {
            return messages.plain(locale, Message.DURATION_PERMANENT);
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return messages.plain(locale, Message.DURATION_SECONDS, "value", "0");
        }
        return format(remaining, messages, locale);
    }

    public static String date(Instant instant) {
        if (instant == null) {
            return "-";
        }
        return DATE.format(instant);
    }
}
