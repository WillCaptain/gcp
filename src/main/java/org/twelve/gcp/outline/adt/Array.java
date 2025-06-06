package org.twelve.gcp.outline.adt;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.Array_;
import org.twelve.gcp.outline.projectable.Reference;

public class Array extends ProductADT{
    private final Node node;
    private final Outline itemOutline;

    public Array(Node node, Outline itemOutline) {
        super(Array_.instance());
        this.node = node;
        this.itemOutline = itemOutline;
    }

    public Outline itemOutline(){
        return this.itemOutline;
    }
    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public String toString() {
        return "["+itemOutline+"]";
    }

    @Override
    public Outline project(Pair<Reference,Outline>[] projections) {
        Outline projection = this.itemOutline;
        for (Pair<Reference, Outline> p : projections) {
            if(p.key().id()==this.itemOutline.id()){
                projection = p.value();
                break;
            }
        }
        if(projection==this.itemOutline) return this;
        return new Array(this.node,projection);
    }
}
