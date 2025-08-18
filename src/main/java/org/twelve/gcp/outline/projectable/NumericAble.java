package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.common.Tool.getExactNumberOutline;

public class NumericAble implements Projectable {
    protected long id;
    protected final BinaryExpression node;
    private final Outline left;
    private final Outline right;
    private AST ast;

    public NumericAble(Outline left, Outline right, BinaryExpression node) {
        this.node = node;
        this.ast = node.ast();
        this.id = node.ast().Counter.getAndIncrement();

        this.left = left;
        this.right = right;
    }

    @Override
    public AST ast() {
        return this.ast;
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public NumericAble copy() {
        return new NumericAble(left,right,node);
    }

    @Override
    public NumericAble copy(Map<Outline, Outline> cache){
        NumericAble copied = cast(cache.get(this));
        if(copied==null){
            copied = new NumericAble(left.copy(cache),right.copy(cache),node);
            cache.put(this,copied);
        }
        return copied;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline l = left instanceof Genericable<?,?> ? ((Genericable<?,?>) left).project(projected, projection,session) : left;
        Outline r = right instanceof Genericable<?,?> ? ((Genericable<?,?>) right).project(projected, projection,session) : right;
        if (l instanceof Genericable<?,?> || r instanceof Genericable<?,?>) {
            return new NumericAble(l, r, cast(this.node));
        }
        return getExactNumberOutline(l, r);
    }

    @Override
    public Outline guess() {
        return this.ast().Number;
    }

    @Override
    public boolean emptyConstraint() {
        return (this.left instanceof Projectable && ((Projectable) this.left).emptyConstraint()) ||
                ((this.right instanceof Projectable && ((Projectable) this.right).emptyConstraint()));
    }

    @Override
    public boolean containsGeneric() {
        return (this.left instanceof Projectable && ((Projectable) this.left).containsGeneric()) ||
                ((this.right instanceof Projectable && ((Projectable) this.right).containsGeneric()));
    }

}
