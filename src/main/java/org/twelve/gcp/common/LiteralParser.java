package org.twelve.gcp.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Utility class for parsing string literals into their corresponding Java objects.
 * Supports booleans, integers, floating-point numbers, decimals, and strings.
 *
 * Features:
 * - Thread-safe implementation (stateless, no instance fields)
 * - Comprehensive number format support
 * - Strict input validation
 * - Graceful fallback to string when parsing fails
 */
public final class LiteralParser {
    private LiteralParser() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // Compile patterns once for better performance
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");
    private static final Pattern LONG_PATTERN = Pattern.compile("-?\\d+[lL]");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("-?\\d*\\.\\d+[fF]");
    private static final Pattern DOUBLE_PATTERN =  Pattern.compile("-?\\d*\\.\\d+[dD]");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("-?\\d*\\.\\d+");
    private static final Pattern SCIENTIFIC_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?[eE][+-]?\\d+");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern HEX_PATTERN = Pattern.compile("0[xX][0-9a-fA-F]+");
    private static final Pattern BINARY_PATTERN = Pattern.compile("0[bB][01]+");

    /**
     * Parses a string literal into the most appropriate Java object.
     *
     * @param lexeme The string to parse (non-null)
     * @return Parsed value (Boolean, Number, String, etc.)
     * @throws IllegalArgumentException if lexeme is null
     */
    public static Object parse(String lexeme) {
        if (lexeme == null) {
            throw new IllegalArgumentException("Lexeme cannot be null");
        }

        // Trim whitespace for more flexible input handling
        String trimmed = lexeme.trim();

        // Check for boolean
        if (BOOLEAN_PATTERN.matcher(trimmed).matches()) {
            return Boolean.parseBoolean(trimmed);
        }

        // Check for quoted strings first
        if (STRING_PATTERN.matcher(trimmed).matches()) {
            return parseStringLiteral(trimmed);
        }

        // Check for numeric types in order of decreasing specificity
        try {
            if (HEX_PATTERN.matcher(trimmed).matches()) {
                return parseHex(trimmed);
            }

            if (BINARY_PATTERN.matcher(trimmed).matches()) {
                return parseBinary(trimmed);
            }

            if (LONG_PATTERN.matcher(trimmed).matches()) {
                return parseLong(trimmed);
            }

            if (FLOAT_PATTERN.matcher(trimmed).matches()) {
                return parseFloat(trimmed);
            }

            if (DOUBLE_PATTERN.matcher(trimmed).matches() ||
                    SCIENTIFIC_PATTERN.matcher(trimmed).matches()) {
                return parseDouble(trimmed);
            }

            if (DECIMAL_PATTERN.matcher(trimmed).matches()) {
                return new BigDecimal(trimmed);
            }

            if (INTEGER_PATTERN.matcher(trimmed).matches()) {
                return parseInteger(trimmed);
            }
        } catch (NumberFormatException e) {
            // Fall through to return as string
        }

        // Default to string representation
        return trimmed;
    }

    // Helper methods for specific types
    private static Long parseLong(String s) {
        return Long.parseLong(s.substring(0, s.length() - 1));
    }

    private static Float parseFloat(String s) {
        return Float.parseFloat(s.substring(0, s.length() - 1));
    }

    private static Double parseDouble(String s) {
        String num = s.endsWith("d") || s.endsWith("D") ?
                s.substring(0, s.length() - 1) : s;
        return Double.parseDouble(num);
    }

    private static Integer parseInteger(String s) {
        return Integer.parseInt(s);
    }

    private static String parseStringLiteral(String s) {
        return s.substring(1, s.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static Number parseHex(String s) {
        return new BigInteger(s.substring(2), 16);
    }

    private static Number parseBinary(String s) {
        return new BigInteger(s.substring(2), 2);
    }

}