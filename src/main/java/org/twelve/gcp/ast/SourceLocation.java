package org.twelve.gcp.ast;

import java.util.Objects;

/**
 * A source location that carries both absolute character offsets AND the
 * human-readable line / column information produced by the lexer.
 *
 * <p>Use this instead of {@link SimpleLocation} whenever a token is converted
 * from the MSLL lexer so that error messages can show {@code "line 3:7"} rather
 * than a raw character offset.
 *
 * @param start absolute character offset (inclusive) of the first character
 * @param end   absolute character offset (inclusive) of the last character
 * @param line  1-based line number
 * @param col   0-based column offset from the start of the line
 *
 * @author huizi 2025
 */
public record SourceLocation(long start, long end, int line, int col) implements Location {

    @Override
    public int line() { return line; }

    @Override
    public int col() { return col; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location that)) return false;
        return start() == that.start() && end() == that.end();
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "line " + line + ":" + col;
    }
}
