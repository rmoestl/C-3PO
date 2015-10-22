package org.c_3po.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Utility class for common string operations.
 * A lot of third-party substitutes are available (e.g. apache commons' StringUtils)
 * but by now it's not worth to drag in an additional dependency.
 */
public class StringUtils {
    // Make it non-instantiable and prohibit subclassing.
    private StringUtils() {
        throw new AssertionError();
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Joins the passed strings with the given delimiter but trims delimiter occurences
     * in input strings to ensure that only one delimiter instance is between two
     * consecutive strings
     *
     * @param delimiter
     * @param strings
     * @return a string resulting from joining strings with the given delimiter without duplicate delimiters
     */
    public static String trimmedJoin(String delimiter, String... strings) {
        StringJoiner stringJoiner = new StringJoiner(Objects.requireNonNull(delimiter));
        Arrays.stream(strings).forEach(s -> stringJoiner.add(s.replaceAll("[" + delimiter + "]", "")));
        return stringJoiner.toString();
    }
}
