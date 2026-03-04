package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata for a single import specifier.
 */
public record ImportMeta(String symbol, String as, String from, String description) {

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("symbol", symbol);
        if (as != null) m.put("as", as);
        m.put("from", from);
        if (description != null) m.put("description", description);
        return m;
    }
}
