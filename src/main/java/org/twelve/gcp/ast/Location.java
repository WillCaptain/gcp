package org.twelve.gcp.ast;

import java.io.Serializable;

import java.util.Objects;

/**
 * Represents a span of positions in source code with utility methods for
 * location queries and comparisons. Supports serialization for distributed use.
 *
 * Example Usage:
 * {@code
 * Location loc = new SimpleLocation(10, 20);
 * if (loc.contains(15)) { ... }
 * }
 * @author huizi 2025
 */
public interface Location extends Serializable {
    long start();
    long end();

    /**
     * Checks if this location contains the given offset.
     * @param offset The position to check (inclusive)
     * @return true if offset is within [start, end]
     */
    default boolean contains(long offset) {
        return offset >= start() && offset <= end();
    }

    /**
     * Checks if this location overlaps with another location.
     * @param other The other location to compare with
     * @return true if the ranges intersect
     */
    default boolean overlaps(Location other) {
        Objects.requireNonNull(other, "Other location cannot be null");
        return this.start() <= other.end() && this.end() >= other.start();
    }

    /**
     * Calculates the length of this location span.
     * @return end - start + 1 (for inclusive ranges)
     */
    default long length() {
        return end() - start() + 1;
    }

    /**
     * Standard equality comparison for locations.
     * Implementations should override for value equality.
     */
    @Override
    boolean equals(Object obj);

    /**
     * Consistent with equals() for hash-based collections.
     * Implementations should override.
     */
    @Override
    int hashCode();

    /**
     * Optional: Creates a new location with merged spans
     * @param other Adjacent/overlapping location
     * @return Combined location range
     * @throws IllegalArgumentException if locations are disjoint
     */
    default Location merge(Location other) {
        if (!this.overlaps(other) && !this.isAdjacent(other)) {
            throw new IllegalArgumentException("Cannot merge disjoint locations");
        }
        return new SimpleLocation(
                Math.min(this.start(), other.start()),
                Math.max(this.end(), other.end())
        );
    }

    /**
     * Checks if this location is adjacent to another (no gap between end+1 and next start)
     */
    default boolean isAdjacent(Location other) {
        return this.end() + 1 == other.start() || other.end() + 1 == this.start();
    }
}