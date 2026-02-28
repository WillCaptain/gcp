package org.twelve.gcp.interpreter;

import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.Value;

/**
 * A specialized lexical environment for entity method calls.
 * <p>
 * Bare field names in an entity method body are resolved through the entity's
 * current fields rather than through the static lexical closure. This ensures
 * that reading {@code data} and {@code this.data} are semantically identical
 * inside entity methods, and assigning {@code data = value} updates the entity
 * field directly.
 * <p>
 * Lookup order: "this" → entity live fields → lexical closure chain.
 * Write order: entity field (if exists) → otherwise normal scope chain.
 */
public class EntityMethodEnvironment extends Environment {

    private final EntityValue entity;
    private final EntityValue thisValue;

    public EntityMethodEnvironment(Environment closure, EntityValue entity) {
        super(closure);
        this.entity = entity;
        this.thisValue = entity;
    }

    @Override
    public Value lookup(String name) {
        if ("this".equals(name)) return thisValue;

        Value field = entity.get(name);
        if (field != null) return field;

        Environment p = parent();
        return p != null ? p.lookup(name) : null;
    }

    /**
     * Entity fields are "owned" by this environment: child scopes that call
     * {@code set(name, value)} will find this env as the owner for any entity
     * field, causing writes to go through {@link #putBinding} instead of
     * updating a stale variable in the lexical closure.
     */
    @Override
    protected Environment findOwner(String name) {
        if ("this".equals(name)) return this;
        if (entity.has(name)) return this;
        return super.findOwner(name);
    }

    @Override
    protected void putBinding(String name, Value value) {
        if ("this".equals(name)) {
            // "this" is immutable at the environment level
            return;
        }
        if (entity.has(name)) {
            entity.setField(name, value);
        } else {
            super.putBinding(name, value);
        }
    }
}
