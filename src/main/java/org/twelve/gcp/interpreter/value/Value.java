package org.twelve.gcp.interpreter.value;

/**
 * Runtime value produced by the GCP interpreter.
 * Every expression evaluates to a Value.
 */
public interface Value {

    /** Returns the underlying Java object (String, Long, Double, etc.). */
    Object unwrap();

    /** Whether this value is truthy in boolean contexts. */
    boolean isTruthy();

    /** Human-readable representation. */
    String display();
}
