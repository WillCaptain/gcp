package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.primitive.NUMBER;

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
    public AbstractNode node() {
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

    /**
     * Resolves an operand to its best-known concrete type.
     * For Genericable operands, prefers the upper bound (extendToBe / max) when available,
     * falling back to guess(). This allows arithmetic-result types to compare correctly
     * against concrete numeric types once their operand generics are constrained.
     */
    private Outline resolveOperand(Outline operand) {
        if (operand instanceof Genericable<?, ?>) {
            Genericable<?, ?> g = cast(operand);
            Outline max = g.max();
            if (!(max instanceof NOTHING)) return max;
        }
        if (operand instanceof Projectable) {
            return ((Projectable) operand).guess();
        }
        return operand;
    }

    /**
     * Resolves both operands and delegates to getExactNumberOutline when both are concrete.
     * This allows {@code a - b} (where a, b: Integer) to compare as Integer.
     */
    @Override
    public boolean tryIamYou(Outline another) {
        Outline l = resolveOperand(left);
        Outline r = resolveOperand(right);
        if (l instanceof NUMBER && r instanceof NUMBER) {
            return getExactNumberOutline(l, r).is(another);
        }
        return false;
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
