package org.twelve.gcp.node.unpack;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.List;

abstract class Field {
    private final Identifier field;

    Field(Identifier field) {
        this.field = field;
    }

    public Identifier field() {
        return this.field;
    }

    public abstract List<Identifier> identifiers();

    public abstract void assign(LocalSymbolEnvironment env, Outline outline);

    abstract Outline outline();
}
