package org.twelve.gcp.interpreter.value;

/** Boolean runtime value. */
public final class BoolValue implements Value {
    public static final BoolValue TRUE  = new BoolValue(true);
    public static final BoolValue FALSE = new BoolValue(false);

    private final boolean value;

    private BoolValue(boolean value) {
        this.value = value;
    }

    public static BoolValue of(boolean b) { return b ? TRUE : FALSE; }

    public boolean value() { return value; }

    @Override public Object unwrap() { return value; }
    @Override public boolean isTruthy() { return value; }
    @Override public String display() { return Boolean.toString(value); }

    @Override public String toString() { return display(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoolValue)) return false;
        return value == ((BoolValue) o).value;
    }

    @Override public int hashCode() { return Boolean.hashCode(value); }
}
