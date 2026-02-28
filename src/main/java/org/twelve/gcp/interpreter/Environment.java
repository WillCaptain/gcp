package org.twelve.gcp.interpreter;

import org.twelve.gcp.interpreter.value.Value;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lexical activation record for the GCP interpreter.
 * <p>
 * Each function call, block, or entity body creates a new child Environment
 * that delegates unknown lookups to its parent – implementing JavaScript-style
 * lexical scoping and closure capture.
 * </p>
 */
public class Environment {

    private final Map<String, Value> bindings = new LinkedHashMap<>();
    private final Environment parent;

    /** Root (global) environment. */
    public Environment() {
        this.parent = null;
    }

    /** Child environment extending a parent. */
    public Environment(Environment parent) {
        this.parent = parent;
    }

    public Environment parent() { return parent; }

    /**
     * Looks up a variable by name, walking the scope chain.
     * Returns {@code null} if not found anywhere.
     */
    public Value lookup(String name) {
        if (bindings.containsKey(name)) return bindings.get(name);
        if (parent != null) return parent.lookup(name);
        return null;
    }

    /**
     * Defines a new variable in the current (innermost) scope.
     */
    public void define(String name, Value value) {
        bindings.put(name, value);
    }

    /**
     * Updates an existing binding anywhere in the scope chain.
     * If not found, creates a new binding in the current scope.
     */
    public void set(String name, Value value) {
        Environment env = findOwner(name);
        if (env != null) {
            env.putBinding(name, value);
        } else {
            bindings.put(name, value);
        }
    }

    protected Environment findOwner(String name) {
        if (bindings.containsKey(name)) return this;
        if (parent != null) return parent.findOwner(name);
        return null;
    }

    /** Writes a value into this environment's own bindings. Subclasses may override to redirect writes. */
    protected void putBinding(String name, Value value) {
        bindings.put(name, value);
    }

    /** Creates a child scope for block-level isolation. */
    public Environment child() {
        return new Environment(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Env{");
        bindings.forEach((k, v) -> sb.append(k).append("=").append(v).append(", "));
        if (parent != null) sb.append("..parent");
        sb.append("}");
        return sb.toString();
    }
}
