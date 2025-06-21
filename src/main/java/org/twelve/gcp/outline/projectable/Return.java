package org.twelve.gcp.outline.projectable;

import lombok.Getter;
import lombok.Setter;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.ANY;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.builtin.NOTHING;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.List;
import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class Return extends Genericable<Return, Node> implements Returnable {
    @Setter
    @Getter
    private Outline argument;

    private Outline supposed = Unknown;

    private Return(Node node, Outline declared) {
        super(node, declared);
    }

    public static Returnable from(Node node, Outline declared) {
        if (declared instanceof Returnable) return cast(declared);
        return new Return(node, declared);
    }

    public static Returnable from(Node node) {
        return from(node, Any);
    }

    public static Returnable from(Outline declared) {
        return from(null, declared);
    }

    /**
     * for high order function
     */
    public static Returnable from() {
        return from(Any);
    }

    public boolean addReturn(Outline returns) {
        if (!(returns instanceof IGNORE) && !returns.is(this.declaredToBe)) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH);
            return false;
        }
        if (supposed instanceof UNKNOWN || (supposed instanceof NOTHING)) {
            supposed = returns;
        } else {
            supposed = Option.from(this.node, supposed, returns);
        }
        return true;
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (this.supposed instanceof UNKNOWN) {
            return super.tryIamYou(another);
        } else {
            return this.supposed.is(another);
        }
    }

    @Override
    protected Outline projectMySelf(Outline projection, ProjectSession session) {
        //todo
        return super.projectMySelf(projection, session);
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        //project myself
        if (projected.id() == this.id()) {
            return this.projectMySelf(projection, session);
        }
        //project related argument in the return constraints
        if (this.argument.id() == projected.id()) {
            if (supposedToBe() instanceof UNKNOWN) {//投影HOF返回值
                return supposedToBe();
            } //else {//投影FOF返回值
            if (supposedToBe() instanceof Projectable) {
                return ((Projectable) supposedToBe()).project(projected, projection, session);
            } else {
                if(supposedToBe() instanceof NOTHING){//in higher function, don't return nothing
                    return this;
                }else {
                    return supposedToBe();
                }
            }
            //}
        }
        //it is an irrelevant projection
        if (supposedToBe() instanceof UNKNOWN) {
            return this;
        } else {
            Return result = this.copy();
            this.projectConstraints(result, projected, projection, session);
            if (!(this.supposedToBe() instanceof UNKNOWN) && (!result.max().is(result.supposedToBe()) || !result.supposedToBe().is(result.min()))) {
                ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this.supposed);
            }
            return result;
        }
    }

    @Override
    protected void projectConstraints(Genericable<?, ?> me, Projectable projected, Outline projection, ProjectSession session) {
        super.projectConstraints(me, projected, projection, session);
        Outline outline = tryProject(((Return) me).supposed, projected, projection, session);
        ((Return) me).supposed =outline;
    }

    @Override
    public boolean equals(Outline another) {
        if (!(another instanceof Return)) return false;
        return this.supposed.equals(((Return) another).supposed);
    }

    @Override
    public Return copy() {
        Return copied = super.copy();
        copied.supposed = this.supposed;
        copied.argument = this.argument;
        return copied;
    }@Override
    public Return copy(Map<Long, Outline> cache) {
        if(cache.containsKey(this.id())) return cast(cache.get(this.id()));
        Return copied = super.copy(cache);
        copied.supposed = this.supposed.copy(cache);
        copied.argument = this.argument;
        return copied;
    }


    public void replaceIgnores() {
        if (this.supposed == Ignore) {
            this.supposed = Unit;
        } else {
            if (!(supposed instanceof Option)) return;
            Option option = cast(supposed);
            option.options().removeIf(o -> o == Ignore);
//            if (option.options().removeIf(o -> o == Ignore)) {
//                option.options().add(Unit);
//            }
        }
    }

    public Outline supposedToBe() {
        if (this.supposed == Nothing && declaredToBe != Any) {
            return declaredToBe;
        } else {
            return this.supposed;
        }
    }

    @Override
    protected Return createNew() {
        return new Return(this.node, this.declaredToBe);
    }

    @Override
    public boolean inferred() {
        return super.inferred() && this.supposedToBe().inferred();
    }

    @Override
    public Outline guess() {

        Outline outline = this.supposed instanceof Projectable ? ((Projectable) this.supposed).guess() : this.supposed;
        outline = (outline instanceof UNKNOWN || outline instanceof NOTHING) ? this.min() : outline;
        return (outline instanceof ANY) ? this.max() : outline;
    }

    /**
     * project reference, it is purely for <T> projection
     * different from project outline
     */
    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        Return projected = cast(super.project(reference, projection));
        projected.supposed = this.supposed.project(reference, projection);
        return projected;
    }

    @Override
    public String toString() {
        String ret;
        if (this.supposed == null || this.supposed instanceof UNKNOWN || this.supposed instanceof NOTHING) {
            ret = super.toString();
        } else {
            ret = this.supposed.toString();
        }
        if(ret.equals("`null`")){
            ret = "?"+this.id;
        }
        return ret;
    }
}
