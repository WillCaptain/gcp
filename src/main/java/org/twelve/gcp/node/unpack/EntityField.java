package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Generic;
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
        ids.add(this.as);
        return ids;
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline outline) {
        this.as.assign(env, outline);
    }

    @Override
    public Outline infer(Inferences inferences) {
        return this.field().infer(inferences);
    }

//    @Override
//    Outline outline() {
//        return Generic.from(this.field(),null);
//    }
}
