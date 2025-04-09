package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.Outline;

/**
 * 传统泛型
 */
public class Reference implements Projectable{
    private long id;
    private final ProductADT constraint;
    private final Node node;
    private Outline projected = null;

    public Reference(Node node, ProductADT constraint){
        this.node = node;
        this.id = Counter.getAndIncrement();

        this.constraint = constraint;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public Reference copy() {
        return new Reference(node,constraint);
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if(projected !=this) return this;//not me
        if(this.projected!=null){
            if(projection.is(this.projected)){
                return this.projected;
            }else {
                return this;
            }
        }
        if(projection.is(this.constraint)){
            this.projected = projection;
            return this.projected;
        }else {
            return this;
        }
    }

    @Override
    public Outline guess() {
        return null;//todo
    }
}
