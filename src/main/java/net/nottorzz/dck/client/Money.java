package net.nottorzz.dck.client;

import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Currency parsing + formatting helpers.
 *
 * <p>Parsing is deliberately tolerant so it can cope with however a server
 * happens to print money: {@code $1,234,567}, {@code 12.5k}, {@code 3.2m},
 * plain {@code 9000}, and so on.</p>
 */
public final class Money {
    private Money() {
    }

    // number (with optional thousands separators / decimals) + optional k/m/b/t suffix
    private static final Pattern TOKEN =
            Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmbtKMBT])?");

    /** Find the largest currency-looking value in the text, if any. */
    public static OptionalDouble parseLargest(String text) {
        if (text == null || text.isEmpty()) {
            return OptionalDouble.empty();
        }
        Matcher m = TOKEN.matcher(text);
        double best = Double.NEGATIVE_INFINITY;
        boolean found = false;
        while (m.find()) {
            String num = m.group(1).replace(",", "");
            if (num.isEmpty() || num.equals(".")) {
                continue;
            }
            double v;
            try {
                v = Double.parseDouble(num);
            } catch (NumberFormatException e) {
                continue;
            }
            String suffix = m.group(2);
            if (suffix != null) {
                switch (Character.toLowerCase(suffix.charAt(0))) {
                    case 'k' -> v *= 1_000d;
                    case 'm' -> v *= 1_000_000d;
                    case 'b' -> v *= 1_000_000_000d;
                    case 't' -> v *= 1_000_000_000_000d;
                    default -> { }
                }
            }
            if (v > best) {
                best = v;
                found = true;
            }
        }
        return found ? OptionalDouble.of(best) : OptionalDouble.empty();
    }

    /** Format a value as a grouped dollar string, e.g. {@code $1,234,567}. */
    public static String format(double v) {
        if (v < 0) {
            v = 0;
        }
        long whole = (long) Math.floor(v);
        double frac = v - whole;
        if (whole < 1000 && frac > 0.0001) {
            return "$" + String.format("%,.2f", v);
        }
        return "$" + String.format("%,d", whole);
    }
}
