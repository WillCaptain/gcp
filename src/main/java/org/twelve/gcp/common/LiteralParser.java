package org.twelve.gcp.common;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class LiteralParser {
    private LiteralParser() {
    }

    // Regular expressions for identifying patterns
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");
    private static final Pattern LONG_PATTERN = Pattern.compile("-?\\d+[lL]");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("-?\\d*\\.\\d+[fF]");
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("-?\\d*\\.\\d+[dD]");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("-?\\d*\\.\\d+");

    public static Object parse(String lexeme) {
        // Check for boolean
        if ("true".equalsIgnoreCase(lexeme)) return true;
        if ("false".equalsIgnoreCase(lexeme)) return false;

        // Check for specific numeric types with suffixes or patterns

        // Check for long type (e.g., "10L")
        if (LONG_PATTERN.matcher(lexeme).matches()) {
            try {
                return Long.parseLong(lexeme.substring(0, lexeme.length() - 1));
            } catch (NumberFormatException ignored) {
            }
        }

        // Check for float type (e.g., "10.5f")
        if (FLOAT_PATTERN.matcher(lexeme).matches()) {
            try {
                return Float.parseFloat(lexeme.substring(0, lexeme.length() - 1));
            } catch (NumberFormatException ignored) {
            }
        }

        // Check for double type (e.g., "10.5d" or "10.5D")
        if (DOUBLE_PATTERN.matcher(lexeme).matches()) {
            try {
                return Double.parseDouble(lexeme.substring(0, lexeme.length() - 1));
            } catch (NumberFormatException ignored) {
            }
        }

        // Check for BigDecimal for high precision decimal values without suffix (decimal type)
        if (DECIMAL_PATTERN.matcher(lexeme).matches()) {
            try {
                return new BigDecimal(lexeme);
            } catch (NumberFormatException ignored) {
            }
        }

        // Check for integer type
        if (INTEGER_PATTERN.matcher(lexeme).matches()) {
            try {
                return Integer.parseInt(lexeme);
            } catch (NumberFormatException ignored) {
            }
        }

        //check exclusive string
        if(lexeme.startsWith("\"") && lexeme.endsWith("\"")){
            return lexeme.replace("\"","");
        }
        // If none of the above, treat as a string
        return lexeme;
    }
}
