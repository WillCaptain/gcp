package org.twelve.gcp.interpreter.value;

import java.util.Objects;

/** Double/Float runtime value. */
public final class FloatValue implements Value {
    private final double value;

    public FloatValue(double value) {
        this.value = value;
    }

    public double value() { return value; }

    @Override public Object unwrap() { return value; }
    @Override public boolean isTruthy() { return value != 0.0; }
    @Override public String display() { return Double.toString(value); }

    @Override public String toString() { return display(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatValue)) return false;
        return Double.compare(((FloatValue) o).value, value) == 0;
    }

    @Override public int hashCode() { return Objects.hashCode(value); }
}
