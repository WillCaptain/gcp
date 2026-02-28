package org.twelve.gcp.node.unpack;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.ArrayList;
import java.util.List;

class EntityField extends Field {
    private final Identifier as;

    EntityField(Identifier field, Identifier as) {
        super(field);
        this.as = as == null ? field : as;
    }

    public Identifier as() {
        return this.as;
    }

    @Override
    public String toString() {
        if (this.field().equals(this.as)) {
            return this.field().toString();
        } else {
            return this.field().toString() + " as " + this.as.toString();
        }
    }

    @Override
    public List<Identifier> identifiers() {
        List<Identifier> ids = new ArrayList<>();
        // Include the field-name identifier so UnpackNodeInference pre-defines it in the
        // environment, allowing IdentifierInference to resolve it during pattern inference.
        if (this.field() != this.as) {
            ids.add(this.field());
        }
        ids.add(this.as);
        return ids;
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline outline) {
        // Always resolve the field name (e.g. 'age') so its AST node gets a type.
        this.field().assign(env, outline);
        // Also resolve the alias binding (e.g. 'a'). When there is no alias, as==field,
        // so this is a no-op duplicate — harmless.
        this.as.assign(env, outline);
    }

    @Override
    public Outline infer(Inferencer inferencer) {
        // 'as' is always set (defaults to 'field' when no alias).
        // After assign(), 'as' is defined in the environment with the field's type,
        // whereas 'field' (the original name) is only defined if it equals 'as'.
        return this.as().infer(inferencer);
    }

//    @Override
//    Outline outline() {
//        return Generic.from(this.field(),null);
//    }
}
