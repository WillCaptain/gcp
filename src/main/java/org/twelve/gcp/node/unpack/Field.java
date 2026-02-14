package org.twelve.gcp.node.unpack;

import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.List;

public abstract class Field {
    private final Identifier field;

    Field(Identifier field) {
        this.field = field;
    }

    public Identifier field() {
        return this.field;
    }

    public abstract List<Identifier> identifiers();

    public abstract void assign(LocalSymbolEnvironment env, Outline outline);

    abstract public Outline infer(Inferences inferences);
    //abstract Outline outline();
}
