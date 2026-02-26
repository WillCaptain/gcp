package org.twelve.gcp.interpreter.value;

import java.util.Objects;

/** Integer/Long runtime value. */
public final class IntValue implements Value {
    public static final IntValue ZERO = new IntValue(0L);

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
