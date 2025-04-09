package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.primitive.STRING;

import static org.twelve.gcp.common.Tool.getExactNumberOutline;

/**
 * add binary expression 可投影outline
 */
public class Addable implements Projectable, OperateAble {
    private long id;
    private final Outline left;
    private final Outline right;
    private Outline definedToBe = null;
    private Node node;

    public Addable(Node node, Outline left, Outline right) {
        this.node = node;
        this.id = Counter.getAndIncrement();

        this.left = left;
        this.right = right;
        this.node = node;
    }
    @Override
    public long id() {
        return this.id;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if (this.definedToBe != null && !projection.is(this.definedToBe)) {
//            ErrorReporter.report(projectionNode, GCPErrCode.OUTLINE_MISMATCH);
            return Outline.Error;
        }
        Outline l = left instanceof Projectable ? ((Projectable) left).project(projected, projection, session) : left;
        Outline r = right instanceof Projectable ? ((Projectable) right).project(projected, projection, session) : right;
        if (l instanceof Projectable || r instanceof Projectable) {
            return new Addable(node, l, r);
        }
        if (l instanceof STRING || r instanceof STRING) {
            return String;
        }
        if (l == Outline.Error || r == Outline.Error) {
            return Outline.Error;
        }
        return getExactNumberOutline(l, r);
    }

    @Override
    public Outline guess() {
        return Option.StringOrNumber;
    }

    @Override
    public Addable copy() {
        return new Addable(node,left,right);
    }

    @Override
    public void addDefinedToBe(Outline outline) {
        if (left instanceof OperateAble) {
            ((OperateAble) left).addDefinedToBe(outline);
        }
        if (right instanceof OperateAble) {
            ((OperateAble) right).addDefinedToBe(outline);
        }
    }

    @Override
    public Node node() {
        return this.node;
    }

    @Override
    public void addHasToBe(Outline outline) {
        if (left instanceof OperateAble) {
            ((OperateAble) left).addHasToBe(outline);
        }
        if (right instanceof OperateAble) {
            ((OperateAble) right).addHasToBe(outline);
        }
    }

    @Override
    public boolean tryIamYou(Outline another) {
//        return another instanceof Addable;
        if (another instanceof Addable) return true;
        return Outline.String.is(another) && Outline.Number.is(another);
    }

    @Override
    public boolean inferred() {
        return this.left.inferred() && this.right.inferred();
    }

    @Override
    public String toString() {
        return  "(str|num)";
    }
}
