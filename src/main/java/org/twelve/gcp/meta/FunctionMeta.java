package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for a function declaration (let f = x -> ...).
 *
 * <pre>
 * let add = (x,y) -> x+y;
 *   → name="add", type="? -> ?", parameters=[{name:x}, {name:y}], returns="?"
 * </pre>
 */
public final class FunctionMeta extends SchemaMeta {

    private final List<FieldMeta> parameters;
    private final String returns;

    public FunctionMeta(String name, String type, String description,
                        List<FieldMeta> parameters, String returns) {
        super(name, Kind.FUNCTION, type, description);
        this.parameters = parameters != null ? parameters : List.of();
        this.returns = returns;
    }

    public List<FieldMeta> parameters() { return parameters; }
    public String returns() { return returns; }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        m.put("parameters", parameters.stream().map(FieldMeta::toMap).toList());
        if (returns != null) m.put("returns", returns);
        return m;
    }
}
