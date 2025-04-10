package org.twelve.gcp.common;

/**
 * An immutable tuple of two elements of potentially different types.
 * @param <K> the type of the first element (key)
 * @param <V> the type of the second element (value)
 * @author huizi 2025
 */
public record Pair<K, V>(K key, V value) {
}
