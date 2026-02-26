package org.twelve.gcp.interpreter.value;

/** Unit (void/nothing) runtime value – singleton. */
public final class UnitValue implements Value {
    public static final UnitValue INSTANCE = new UnitValue();

    private UnitValue() {}

    @Override public Object unwrap() { return null; }
    @Override public boolean isTruthy() { return false; }
    @Override public String display() { return "()"; }

    @Override public String toString() { return "()"; }
}
