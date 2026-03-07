package org.twelve.gcp.interpreter.value;

import java.util.Objects;

/** Integer/Long runtime value. */
public final class IntValue implements Value {

    // Small-value cache: avoids heap allocation for the most common integer results.
    // Range covers typical loop counters, array indices, and small arithmetic results.
    private static final int CACHE_LOW  = -256;
    private static final int CACHE_HIGH = 65535;
    private static final IntValue[] CACHE;

    static {
        CACHE = new IntValue[CACHE_HIGH - CACHE_LOW + 1];
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new IntValue((long) (i + CACHE_LOW));
        }
    }

    /** Returns a cached instance for values in [-256, 65535], otherwise allocates. */
    public static IntValue of(long v) {
        if (v >= CACHE_LOW && v <= CACHE_HIGH) {
            return CACHE[(int)(v - CACHE_LOW)];
        }
        return new IntValue(v);
    }

    public static final IntValue ZERO = CACHE[-CACHE_LOW]; // of(0)

    private final long value;

    public IntValue(long value) {
        this.value = value;
    }

    public long value() { return value; }

    @Override public Object unwrap() { return value; }
    @Override public boolean isTruthy() { return value != 0; }
    @Override public String display() { return Long.toString(value); }

    @Override public String toString() { return display(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntValue)) return false;
        return value == ((IntValue) o).value;
    }

    @Override public int hashCode() { return Objects.hashCode(value); }
}
