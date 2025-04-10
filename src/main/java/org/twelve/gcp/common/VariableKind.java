package org.twelve.gcp.common;

/**
 * Represents the kind of variable declaration in the language.
 *
 * <p>The two kinds are:
 * <ul>
 *   <li>{@code VAR} - Mutable variable (can be reassigned)</li>
 *   <li>{@code LET} - Immutable variable (cannot be reassigned after initialization)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * VariableKind kind = VariableKind.LET;
 * if (kind.isImmutable()) {
 *     System.out.println("This variable cannot be reassigned");
 * }
 * }</pre>
 *
 * @author huizi 2025
 */
public enum VariableKind {
    /**
     * Mutable variable declaration (can be reassigned).
     * Equivalent to 'var' in JavaScript or non-final in Java.
     */
    VAR,

    /**
     * Immutable variable declaration (cannot be reassigned).
     * Equivalent to 'let' in JavaScript or 'final' in Java.
     */
    LET;

    /**
     * Checks if this variable kind is mutable.
     *
     * @return true for VAR, false for LET
     */
    public boolean mutable() {
        return this == VAR;
    }

    public static VariableKind from(Boolean mutable) {
        return mutable ? VAR : LET;
    }
}
