package org.twelve.gcp.interpreter.value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Dictionary (map) runtime value. */
public final class DictValue implements Value {
    private final LinkedHashMap<Value, Value> entries;

    public DictValue(Map<Value, Value> entries) {
        this.entries = new LinkedHashMap<>(entries);
    }

    public Map<Value, Value> entries() { return entries; }

    public Value get(Value key) { return entries.get(key); }

    public void put(Value key, Value val) { entries.put(key, val); }

    public int size() { return entries.size(); }

    public boolean containsKey(Value key) { return entries.containsKey(key); }

    public java.util.List<Value> keys() { return new java.util.ArrayList<>(entries.keySet()); }

    public java.util.List<Value> values() { return new java.util.ArrayList<>(entries.values()); }

    @Override public Object unwrap() { return entries; }
    @Override public boolean isTruthy() { return !entries.isEmpty(); }

    @Override
    public String display() {
        return "[" + entries.entrySet().stream()
                .map(e -> e.getKey().display() + ": " + e.getValue().display())
                .collect(Collectors.joining(", ")) + "]";
    }

    @Override public String toString() { return display(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictValue)) return false;
        return entries.equals(((DictValue) o).entries);
    }

    @Override public int hashCode() { return entries.hashCode(); }
}
