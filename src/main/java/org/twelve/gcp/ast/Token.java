package org.twelve.gcp.ast;

import org.twelve.gcp.common.CONSTANTS;

import java.util.Objects;

/**
 * Represents a lexical token with associated source location and data.
 *
 * Features:
 * - Immutable design for thread safety
 * - Special handling for system-generated tokens (location = -1)
 * - Unit token singleton pattern
 *
 * @param <T> Type of token data (typically String or enum)
 */
public class Token<T> {
    // Singleton unit token
    private static final Token<String> UNIT = new Token<>(CONSTANTS.UNIT, 0);

    private final T data;
    private final Location loc;

    /**
     * Creates a token with calculated location span.
     * @param data Token content (non-null)
     * @param start Starting offset in source (inclusive)
     */
    public Token(T data, int start) {
        this.data = Objects.requireNonNull(data, "Token data cannot be null");
        this.loc = new SimpleLocation(
                start,
                start + data.toString().length() - 1  // Convert length to inclusive end pos
        );
    }

    /**
     * Creates a token with full source location including line / column.
     * Prefer this constructor when converting MSLL lexer tokens so that
     * error messages can show {@code "line N:M"} instead of a raw offset.
     *
     * @param data  Token content (non-null)
     * @param start Absolute character offset of the first character
     * @param line  1-based line number
     * @param col   0-based column offset from the start of the line
     */
    public Token(T data, int start, int line, int col) {
        this.data = Objects.requireNonNull(data, "Token data cannot be null");
        this.loc = new SourceLocation(
                start,
                start + data.toString().length() - 1,
                line,
                col);
    }

    /**
     * Creates a token from an existing {@link Location}.
     * Useful when re-wrapping a location that already carries full source info.
     */
    public Token(T data, Location loc) {
        this.data = Objects.requireNonNull(data, "Token data cannot be null");
        this.loc = loc;
    }

    /**
     * Creates a system-generated token without valid location.
     * @param lexeme Token content (non-null)
     */
    public Token(T lexeme) {
        this(lexeme, -1);  // Mark as synthetic token
    }

    // --- Core Accessors ---
    public Location loc() {
        return this.loc;
    }

    public String lexeme() {
        return this.data.toString();
    }

    public T data() {
        return this.data;
    }

    // --- Utility Methods ---
    /**
     * Checks if this is a synthetic (system-generated) token.
     */
    public boolean isSynthetic() {
        return loc.start() < 0;
    }

    /**
     * Gets the singleton unit token instance.
     */
    public static Token<String> unit() {
        return UNIT;
    }

    // --- Object Overrides ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token<?> token)) return false;
        return Objects.equals(data, token.data) &&
                Objects.equals(loc, token.loc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, loc);
    }

    @Override
    public String toString() {
        return String.format("Token[%s@%s]",
                data,
                isSynthetic() ? "SYNTHETIC" : loc);
    }

    // --- Design Considerations ---
    /*
     * Potential Extensions:
     * 1. Add token type/category metadata
     * 2. Support for line/column locations
     * 3. Builder pattern for complex token creation
     * 4. Flyweight pattern for common tokens
     */
}