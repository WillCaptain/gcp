package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A symbol visible within a scope: variable, outline, function, or parameter.
 * Used for IDE autocomplete and LLM-driven code navigation.
 */
public record SymbolMeta(String name, String type, String kind, boolean mutable) {

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        if (type != null) m.put("type", type);
        m.put("kind", kind);
        m.put("mutable", mutable);
        return m;
    }
}
