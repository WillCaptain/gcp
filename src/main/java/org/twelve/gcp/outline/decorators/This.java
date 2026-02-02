package org.twelve.gcp.outline.decorators;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.ProductADT;

public class This extends ProductADT {
    private final Entity origin;

    public This(Entity origin){
        super(origin.ast(),origin.buildIn());
        this.origin = origin;
    }

    @Override
    public String toString() {
        return "{...}";
    }

    @Override
    public Node node() {
        return this.origin.node();
    }

    @Override
    public boolean containsUnknown() {
        return false;
    }

    public Entity real(){
        return this.origin;
    }
}
