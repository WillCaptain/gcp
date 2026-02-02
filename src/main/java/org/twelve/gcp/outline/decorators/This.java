package org.twelve.gcp.outline.decorators;

import lombok.Setter;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.projectable.FirstOrderFunction;

import java.util.Map;

public class This extends ProductADT {
    @Setter
    private Entity origin;

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

    @Override
    public This copy(Map<Outline, Outline> cache) {
        return new This(this.origin);
    }
}
