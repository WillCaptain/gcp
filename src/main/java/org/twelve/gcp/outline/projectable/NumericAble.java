package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.ONode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.common.Tool.getExactNumberOutline;

public class NumericAble implements Projectable {
    protected long id;
    protected final BinaryExpression node;
    private final Outline left;
    private final Outline right;

    public NumericAble(Outline left, Outline right, BinaryExpression node) {
        this.node = node;
        this.id = Counter.getAndIncrement();

        this.left = left;
        this.right = right;
    }

    @Override
    public ONode node() {
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
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Outline l = left instanceof Generic ? ((Generic) left).project(projected, projection,session) : left;
        Outline r = right instanceof Generic ? ((Generic) right).project(projected, projection,session) : right;
        if (l instanceof Generic || r instanceof Generic) {
            return new NumericAble(l, r, cast(this.node));
        }
        return getExactNumberOutline(l, r);
    }

    @Override
    public Outline guess() {
        return Number;
    }

}
