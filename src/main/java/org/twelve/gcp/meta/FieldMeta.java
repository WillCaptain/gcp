package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schema field: a named member of an outline/entity with a type.
 * For entity fields like {@code name:String} or method-shaped fields like {@code provinces:Unit -> Provinces}.
 *
 * @param origin "own" for members declared directly on this outline,
 *               "base" for members inherited from a base type,
 *               "builtin" for system-provided members like {@code to_str}.
 */
public record FieldMeta(String name, String type, String description, String origin) {

    public FieldMeta(String name, String type, String description) {
        this(name, type, description, null);
    }

    public boolean isMethod() {
        return type != null && type.contains("->");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("type", type);
        if (description != null) m.put("description", description);
        if (origin != null) m.put("origin", origin);
        return m;
    }
}
