package org.twelve.gcp.node.unpack;

import org.twelve.gcp.inference.Inferencer;
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

    /** Returns the nested unpack node if this is a nested-unpack field, otherwise null. */
    public UnpackNode nestedUnpack() { return null; }

    public abstract void assign(LocalSymbolEnvironment env, Outline outline);

    abstract public Outline infer(Inferencer inferencer);
    //abstract Outline outline();
}
