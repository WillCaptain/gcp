package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata for a single export specifier.
 */
public record ExportMeta(String name, String as, String description) {

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        if (as != null) m.put("as", as);
        if (description != null) m.put("description", description);
        return m;
    }
}
