package org.twelve.gcp.interpreter.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Mutable list runtime value. */
public final class ArrayValue implements Value {
    private final List<Value> elements;

    public ArrayValue(List<Value> elements) {
        this.elements = new ArrayList<>(elements);
    }

    public List<Value> elements() { return elements; }

    public Value get(int index) {
        if (index < 0) index = elements.size() + index;
        return elements.get(index);
    }

    public void set(int index, Value v) {
        if (index < 0) index = elements.size() + index;
        elements.set(index, v);
    }

    public int size() { return elements.size(); }

    @Override public Object unwrap() { return elements; }
    @Override public boolean isTruthy() { return !elements.isEmpty(); }

    @Override
    public String display() {
        return "[" + elements.stream().map(Value::display).collect(Collectors.joining(", ")) + "]";
    }

    @Override public String toString() { return display(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArrayValue)) return false;
        return elements.equals(((ArrayValue) o).elements);
    }

    @Override public int hashCode() { return elements.hashCode(); }
}
