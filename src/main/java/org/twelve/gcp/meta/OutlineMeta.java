package org.twelve.gcp.meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for an {@code outline} declaration.
 * <p>
 * Entity-shaped outlines expose {@link #fields()} and {@link #methods()};
 * ADT / primitive outlines may have empty field lists.
 *
 * <pre>
 * outline Country = { id:0, name:String, provinces:Unit -> Provinces };
 *   → name="Country", type="{id:Int, name:String, provinces:Unit -> Provinces}"
 *   → fields: [id:Int, name:String]
 *   → methods: [provinces:Unit -> Provinces]
 * </pre>
 */
public final class OutlineMeta extends SchemaMeta {

    private final List<FieldMeta> allFields;

    public OutlineMeta(String name, String type, String description, List<FieldMeta> allFields) {
        super(name, Kind.OUTLINE, type, description);
        this.allFields = allFields != null ? allFields : List.of();
    }

    /** All fields (data + methods). */
    @Override
    public List<FieldMeta> fields() {
        List<FieldMeta> result = new ArrayList<>();
        for (FieldMeta f : allFields) {
            if (!f.isMethod()) result.add(f);
        }
        return result;
    }

    /** Fields whose type contains {@code ->} (function / method shaped). */
    @Override
    public List<FieldMeta> methods() {
        List<FieldMeta> result = new ArrayList<>();
        for (FieldMeta f : allFields) {
            if (f.isMethod()) result.add(f);
        }
        return result;
    }

    /** All members (fields + methods), preserving declaration order. */
    public List<FieldMeta> members() {
        return allFields;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        if (!allFields.isEmpty()) {
            m.put("fields", fields().stream().map(FieldMeta::toMap).toList());
            m.put("methods", methods().stream().map(FieldMeta::toMap).toList());
        }
        return m;
    }
}
