package org.twelve.gcp.meta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata for a {@code let} / {@code var} declaration.
 *
 * <pre>
 * let countries = __ontology_repo__&lt;Countries&gt;;
 *   → name="countries", kind="let", type="Countries", mutable=false
 * </pre>
 */
public final class VariableMeta extends SchemaMeta {

    private final boolean mutable;

    public VariableMeta(String name, String varKind, String type, boolean mutable, String description) {
        super(name, Kind.VARIABLE, type, description);
        this.mutable = mutable;
    }

    public boolean mutable() { return mutable; }

    /** "let" or "var". */
    public String varKind() {
        return mutable ? "var" : "let";
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> m = super.toMap();
        m.put("mutable", mutable);
        return m;
    }
}
