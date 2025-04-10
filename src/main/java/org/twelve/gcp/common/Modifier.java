package org.twelve.gcp.common;

/**
 * Represents access modifiers in the type system with comparison operations.
 *
 * <p>The modifiers form a simple lattice with:
 * <pre>
 *     PUBLIC (least restrictive)
 *       |
 *     PRIVATE (most restrictive)
 * </pre>
 *
 * <p>Provides operations for finding the most/least restrictive modifier.
 */
public enum Modifier {
    PUBLIC, PRIVATE;

    /**
     * Returns the more permissive (max) of two modifiers.
     * Public is considered more permissive than private.
     *
     * @param another The other modifier to compare with
     * @return The more permissive modifier
     * @throws IllegalArgumentException if argument is null
     */
    public Modifier mostPermissive(Modifier another) {
        if (another == null) {
            throw new IllegalArgumentException("Cannot compare with null modifier");
        }
        return this == PUBLIC ? this : another;
    }

    /**
     * Returns the more restrictive (min) of two modifiers.
     * Private is considered more restrictive than public.
     *
     * @param another The other modifier to compare with
     * @return The more restrictive modifier
     * @throws IllegalArgumentException if argument is null
     */
    public Modifier mostRestrictive(Modifier another) {
        if (another == null) {
            throw new IllegalArgumentException("Cannot compare with null modifier");
        }
        return this == PRIVATE ? this : another;
    }
}
