package org.twelve.gcp.interpreter.value;

import java.util.Objects;

/** String runtime value. */
public final class StringValue implements Value {
    private final String value;

    public StringValue(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public String value() { return value; }

    @Override public Object unwrap() { return value; }
    @Override public boolean isTruthy() { return !value.isEmpty(); }
    @Override public String display() { return value; }

    @Override public String toString() { return "\"" + value + "\""; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringValue)) return false;
        return value.equals(((StringValue) o).value);
    }

    @Override public int hashCode() { return value.hashCode(); }
}
