package org.twelve.gcp.node.unpack;

import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.List;

class NestField extends Field {

    private final UnpackNode nest;

    NestField(Identifier field, UnpackNode nest) {
        super(field);
        this.nest = nest;
    }

    public UnpackNode nest() {
        return this.nest;
    }

    @Override
    public UnpackNode nestedUnpack() { return this.nest; }

    @Override
    public String toString() {
        return this.field().toString() + ": " + this.nest.toString();
    }

    @Override
    public List<Identifier> identifiers() {
        return this.nest().identifiers();
    }

    @Override
    public void assign(LocalSymbolEnvironment env, Outline outline) {
        this.nest.assign(env, outline);
    }

    @Override
    public Outline infer(Inferencer inferencer) {
        return this.nest.infer(inferencer);
    }

//    @Override
//    Outline outline() {
//        return nest.outline();
//    }
}
