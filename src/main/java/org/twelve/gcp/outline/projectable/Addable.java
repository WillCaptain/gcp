package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.NUMBER;
import org.twelve.gcp.outline.primitive.STRING;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.common.Tool.getExactNumberOutline;

/**
 * add binary expression 可投影outline
 */
public class Addable implements Projectable, OperateAble {
    private long id;
    private final Outline left;
    private final Outline right;
    private Outline definedToBe = null;
    private AbstractNode node;

    public Addable(AbstractNode node, Outline left, Outline right) {
        this(node.ast().Counter.getAndIncrement(),node,left,right);
    }

    private Addable(long id, AbstractNode node, Outline left, Outline right) {
        this.node = node;
        this.id = id;

        this.left = left;
        this.right = right;
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        if(this.id()==projected.id()){
            if(projection.is(this)){
                if(left instanceof Projectable ) {
                    if(projection.is(left)) {
                        session.removeProjection(left);
                        ((Projectable) left).project(cast(left), projection, session);
                    }else{
                        ((Projectable) left).project(cast(left), ((Projectable) left).guess(), session);
                    }
                }
                if(right instanceof Projectable) {
                    if(projection.is(right)) {
                        session.removeProjection(right);
                        ((Projectable) right).project(cast(right), projection, session);
                    }else{
                        ((Projectable) right).project(cast(right), ((Projectable) right).guess(), session);
                    }
                }
                return projection;
            }else{
                GCPErrorReporter.report(this.ast(),this.node(), GCPErrCode.OUTLINE_MISMATCH,"add type mismatch");
                return this.guess();
            }
        }else {
            if (this.definedToBe != null && !projection.is(this.definedToBe)) {
//            ErrorReporter.report(projectionNode, GCPErrCode.OUTLINE_MISMATCH);
                return this.node().ast().Error;
            }
            Outline l = left instanceof Projectable ? ((Projectable) left).project(projected, projection, session) : left;
            Outline r = right instanceof Projectable ? ((Projectable) right).project(projected, projection, session) : right;
            if (l instanceof STRING || r instanceof STRING) {
                return this.node().ast().String;
            }
            if (l == this.node().ast().Error || r == this.node().ast().Error) {
                return this.node().ast().Error;
            }
            //if (((l instanceof Projectable) && l.id() != this.left.id()) || ((r instanceof Projectable) && r.id() != this.right.id())) {
            if (l instanceof Projectable || r instanceof Projectable) {
                if (l instanceof Addable || r instanceof Addable) return this;//not sure
                return new Addable(node, l, r);
            }
            return getExactNumberOutline(l, r);
        }
    }

    @Override
    public Outline guess() {
        return this.node().ast().stringOrNumber(node);
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

    @Override
    public Addable copy() {
        return new Addable(this.id,node, left, right);
    }

    @Override
    public Addable copy(Map<Outline, Outline> cache) {
        Addable copied = cast(cache.get(this));
        if (copied == null) {
            copied = new Addable(node, left.copy(cache), right.copy(cache));
            cache.put(this, copied);
        }
        return copied;
    }

    @Override
    public boolean addDefinedToBe(Outline outline) {
        boolean result = true;
        if (left instanceof OperateAble) {
            result = result || ((OperateAble) left).addDefinedToBe(outline);
        }
        if (right instanceof OperateAble) {
            result = result || ((OperateAble) right).addDefinedToBe(outline);
        }
        return result;
    }

    @Override
    public AST ast() {
        return this.node.ast();
    }

    @Override
    public AbstractNode node() {
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
        return this.node().ast().String.is(another) && this.node().ast().Number.is(another);
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        return ((another instanceof NUMBER ||another instanceof STRING ));
    }

    @Override
    public boolean inferred() {
        return this.left.inferred() && this.right.inferred();
    }

    @Override
    public String toString() {
        return "(str|num)";
    }
}
