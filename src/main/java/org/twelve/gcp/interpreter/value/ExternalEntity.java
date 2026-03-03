package org.twelve.gcp.interpreter.value;

import java.util.Map;

/**
 * Marker interface for entity values managed by external plugins (e.g. Entitir).
 *
 * <p>When {@link org.twelve.gcp.interpreter.interpretation.EntityInterpretation}
 * evaluates an entity literal whose base is an {@code ExternalEntity}, it delegates
 * the final construction to {@link #extend(Map)} so the plugin can intercept the
 * full set of evaluated fields (for DB persistence, event emission, etc.).
 *
 * <p>Without this hook, {@code EntityInterpretation} would produce a plain
 * {@link EntityValue} that wraps the plugin value as a base, losing the ability
 * to observe the evaluated fields and to add runtime-dynamic edge methods that
 * close over the correct field values.
 */
public interface ExternalEntity {

    /**
     * Called by {@code EntityInterpretation} with the fully-evaluated field map
     * after the entity literal body has been processed.
     *
     * @param fields  the evaluated fields from the {@code {...}} extension body
     * @return        the final entity value (typically a new instance with
     *                edge methods wired and DB persistence performed)
     */
    EntityValue extend(Map<String, Value> fields);
}
