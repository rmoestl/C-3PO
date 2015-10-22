package org.c_3po.util;

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
}
