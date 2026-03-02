package org.twelve.gcp.interpreter.value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Ordered, fixed-length tuple of values: (v0, v1, …). */
public final class TupleValue implements Value {
    private final List<Value> elements;

    public TupleValue(List<Value> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    public List<Value> elements() { return elements; }

    public Value get(int index) {
        if (index < 0) {
            index = elements.size() + index;
        }
        return elements.get(index);
    }

    public int size() { return elements.size(); }

    @Override public Object unwrap() { return elements; }
    @Override public boolean isTruthy() { return !elements.isEmpty(); }

    /** Human-readable form: strings are unquoted, elements separated by ", ". */
    @Override
    public String display() {
        return "(" + elements.stream().map(Value::display).collect(Collectors.joining(", ")) + ")";
    }

    /** Code-style form: strings are quoted, elements separated by ",". */
    @Override
    public String toString() {
        return "(" + elements.stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TupleValue)) return false;
        return elements.equals(((TupleValue) o).elements);
    }

    @Override public int hashCode() { return elements.hashCode(); }
}
