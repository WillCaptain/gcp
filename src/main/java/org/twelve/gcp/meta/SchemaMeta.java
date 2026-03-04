package org.twelve.gcp.meta;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all top-level declarations in a module's meta schema.
 * Subclasses: {@link OutlineMeta}, {@link VariableMeta}, {@link FunctionMeta}.
 * <p>
 * Every declaration has a name, a kind (outline/variable/function), a type signature
 * (human-readable text), and an optional description from comments.
 */
public sealed class SchemaMeta permits OutlineMeta, VariableMeta, FunctionMeta {

    public enum Kind { OUTLINE, VARIABLE, FUNCTION }

    private final String name;
    private final Kind kind;
    private final String type;
    private final String description;

    protected SchemaMeta(String name, Kind kind, String type, String description) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.description = description;
    }

    public String name() { return name; }
    public Kind kind() { return kind; }
    public String type() { return type; }
    public String description() { return description; }

    /** Override in subclasses to expose entity fields. */
    public List<FieldMeta> fields() { return Collections.emptyList(); }
    /** Override in subclasses to expose methods (function-type fields). */
    public List<FieldMeta> methods() { return Collections.emptyList(); }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("kind", kind.name().toLowerCase());
        m.put("type", type);
        if (description != null) m.put("description", description);
        return m;
    }
}
