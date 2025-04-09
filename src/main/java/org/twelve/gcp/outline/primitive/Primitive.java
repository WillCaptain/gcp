package org.twelve.gcp.outline.primitive;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.builtin.BuildInOutline;

public abstract class Primitive extends ProductADT {

    private ONode node;

    protected Primitive(BuildInOutline buildIn) {
        this(buildIn,null);
    }

    protected Primitive(BuildInOutline buildIn,ONode node) {
        super(buildIn);
        this.node = node;
    }
    @Override
    public long id() {
        return buildIn.id();
    }

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public ONode node() {
        return this.node;
    }
}
