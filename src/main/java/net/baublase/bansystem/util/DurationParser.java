package net.baublase.bansystem.util;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parst 1d, 12h, 30m, kombinierbar (1d12h). permanent/perm/0 = Dauer-Ban.
 */
public final class DurationParser {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Optional<Duration> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String raw = input.trim().toLowerCase(Locale.ROOT);
        if (raw.equals("permanent") || raw.equals("perm") || raw.equals("-1") || raw.equals("0")) {
            return Optional.of(Duration.ZERO);
        }
        Matcher matcher = TOKEN.matcher(raw);
        Duration total = Duration.ZERO;
        int consumed = 0;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                return Optional.empty();
            }
            long amount = Long.parseLong(matcher.group(1));
            total = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "d" -> total.plusDays(amount);
                case "h" -> total.plusHours(amount);
                case "m" -> total.plusMinutes(amount);
                case "s" -> total.plusSeconds(amount);
                default -> total;
            };
            consumed = matcher.end();
        }
        if (consumed == 0 || consumed != raw.length() || total.isZero()) {
            return Optional.empty();
        }
        return Optional.of(total);
    }
}
