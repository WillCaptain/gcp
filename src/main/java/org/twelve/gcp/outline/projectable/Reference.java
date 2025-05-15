package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;

/**
 * 传统泛型
 */
public class Reference implements Projectable{
    private long id;
    private final Outline constraint;
    private final Node node;
    private Outline projected = null;

    public Reference(Identifier node, Outline constraint){
        this.node = node;
        this.id = Counter.getAndIncrement();

        this.constraint = constraint;
    }

    public static Reference from(Identifier node) {
        return from(node,Any);
    }
    public static Reference from(Identifier node, Outline constraint) {
        return new Reference(node,constraint);
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public Reference copy() {
        return new Reference(cast(node),constraint);
    }

    @Override
    public Identifier node() {
        return cast(this.node);
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
    public String toString() {
        return this.node().name();
    }

    @Override
    public Outline guess() {
        return null;//todo
    }
}
