package org.twelve.gcp.interpreter.value;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity (record/object) runtime value.
 * Holds named fields in insertion order; supports inheritance via a base entity.
 * When a field is not found in this entity, the lookup falls through to the base.
 */
public class EntityValue implements Value {

    /** Optional symbol tag (for SymbolEntity values, e.g. Male, Female). */
    private final String symbolTag;

    /** This entity's own fields (package-accessible for mutation during entity construction). */
    final LinkedHashMap<String, Value> fields;

    /** Base entity for inheritance delegation (may be null). */
    private final EntityValue base;

    public EntityValue(Map<String, Value> fields) {
        this(null, fields, null);
    }

    public EntityValue(Map<String, Value> fields, EntityValue base) {
        this(null, fields, base);
    }

    public EntityValue(String symbolTag, Map<String, Value> fields, EntityValue base) {
        this.symbolTag = symbolTag;
        this.fields = new LinkedHashMap<>(fields);
        this.base = base;
    }

    /**
     * Static factory that shares the given mutable LinkedHashMap directly (no copy).
     * Used by the interpreter's entity construction to allow incremental field population
     * while still having a stable 'this' reference for closures.
     */
    public static EntityValue shared(LinkedHashMap<String, Value> sharedFields, EntityValue base) {
        EntityValue ev = new EntityValue(java.util.Collections.emptyMap()); // dummy
        // Bypass the copy in the normal constructor by replacing the fields map
        // This is achieved via a special internal constructor pattern.
        // We use a subclass-free trick: just use the public setField API in a loop.
        // Instead, expose a factory using a marker approach.
        return new EntityValue(sharedFields, base, (String) null);
    }

    // Internal constructor used by shared() factory – symbolTag is ignored (null)
    private EntityValue(LinkedHashMap<String, Value> sharedFields, EntityValue base, String ignored) {
        this.symbolTag = null;
        this.fields = sharedFields; // intentionally not copied
        this.base = base;
    }

    /** Like {@link #shared} but also sets a symbol tag (for symbol-constructor entities). */
    public static EntityValue sharedTagged(LinkedHashMap<String, Value> sharedFields,
                                           EntityValue base, String symbolTag) {
        return new EntityValue(sharedFields, base, symbolTag, true);
    }

    // Full internal constructor
    private EntityValue(LinkedHashMap<String, Value> sharedFields, EntityValue base,
                        String symbolTag, boolean shared) {
        this.symbolTag = symbolTag;
        this.fields = sharedFields;
        this.base = base;
    }

    /** Optional symbol tag (e.g. "Male", "Female", "Dog"). */
    public String symbolTag() { return symbolTag; }

    public boolean hasSymbol() { return symbolTag != null; }

    /** Looks up a field by name; delegates to base if not found locally. */
    public Value get(String name) {
        if (fields.containsKey(name)) return fields.get(name);
        if (base != null) return base.get(name);
        return null;
    }

    /** Checks whether the field exists (own or inherited). */
    public boolean has(String name) {
        return fields.containsKey(name) || (base != null && base.has(name));
    }

    /** Sets (or adds) a field in the own fields map. */
    public void setField(String name, Value value) {
        fields.put(name, value);
    }

    /** Returns own fields only. */
    public Map<String, Value> ownFields() {
        return Collections.unmodifiableMap(fields);
    }

    /** Returns all fields (own + inherited, own overrides inherited). */
    public Map<String, Value> allFields() {
        if (base == null) return ownFields();
        LinkedHashMap<String, Value> all = new LinkedHashMap<>(base.allFields());
        all.putAll(fields);
        return Collections.unmodifiableMap(all);
    }

    /** Returns the base entity (may be null). */
    public EntityValue base() { return base; }

    /** Creates a new entity by extending this entity with additional/overriding fields. */
    public EntityValue extend(Map<String, Value> extra) {
        return new EntityValue(extra, this);
    }

    /** Creates a new entity by extending this entity with a symbol tag. */
    public EntityValue extend(Map<String, Value> extra, String symbol) {
        return new EntityValue(symbol, extra, this);
    }

    @Override public Object unwrap() { return allFields(); }
    @Override public boolean isTruthy() { return true; }

    // Thread-local guard to detect circular entity references in display()
    private static final ThreadLocal<Set<Integer>> DISPLAYING =
            ThreadLocal.withInitial(HashSet::new);

    @Override
    public String display() {
        Set<Integer> visited = DISPLAYING.get();
        int id = System.identityHashCode(this);
        if (!visited.add(id)) return "{...}";
        try {
            String tag = symbolTag != null ? symbolTag : "";
            String body = fields.entrySet().stream()
                    .map(e -> e.getKey() + " = " + e.getValue().display())
                    .collect(Collectors.joining(", "));
            if (base != null) {
                String baseBody = base.display();
                body = body.isEmpty() ? baseBody : baseBody + ", " + body;
            }
            return tag + "{" + body + "}";
        } finally {
            visited.remove(id);
        }
    }

    @Override public String toString() { return display(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityValue)) return false;
        EntityValue other = (EntityValue) o;
        return Objects.equals(symbolTag, other.symbolTag) &&
               Objects.equals(fields, other.fields);
    }

    @Override public int hashCode() {
        return Objects.hash(symbolTag, fields);
    }
}
