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
        if (type == null || type.isBlank()) return false;
        int parenDepth = 0;
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean inString = false;
        for (int i = 0; i < type.length() - 1; i++) {
            char ch = type.charAt(i);
            if (ch == '"' && (i == 0 || type.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) continue;
            switch (ch) {
                case '(' -> parenDepth++;
                case ')' -> parenDepth = Math.max(0, parenDepth - 1);
                case '{' -> braceDepth++;
                case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                default -> {
                    if (ch == '-' && type.charAt(i + 1) == '>'
                            && parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
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
