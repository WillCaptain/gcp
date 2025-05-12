package org.twelve.gcp.outline.projectable;

import lombok.Getter;
import lombok.Setter;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import static org.twelve.gcp.common.Tool.cast;

public class Return extends Genericable<Return, Node> {
    @Setter
    @Getter
    private Outline argument;

    private Outline supposed = Unknown;

    private Return(Node node, Outline declared) {
        super(node, declared);
    }

    public static Return from(Node node, Outline declared) {
        return new Return(node, declared);
    }

    public static Return from(Node node) {
        return new Return(node, Any);
    }

    public static Return from(Outline declared) {
        return new Return(null, declared);
    }

    /**
     * for high order function
     */
    public static Return from() {
        return from(Any);
    }

    public boolean addReturn(Outline returns) {
        if (!(returns instanceof IGNORE) && !returns.is(this.declaredToBe)) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH);
            return false;
        }
        if (supposed instanceof UNKNOWN) {
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
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
//        if (session.getProjection(this) != null) {
//            return session.getProjection(this);
//        }
        if (projected.id() == this.id()) {
            return this.projectMySelf(projection, session);
        }
        //投影对应的参数时，才真正得到投影值
        if (this.argument.id() == projected.id()) {
            if (supposed instanceof UNKNOWN) {//投影HOF返回值
//                return this.projectLambda(this, cast(projection), session);
                return this.projectLambda(cast(this.node().outline()), cast(projection), session);
            } else {//投影FOF返回值
                if (supposed instanceof Projectable) {
                    return ((Projectable) supposed).project(projected, projection, session);
                } else {
                    return supposed;
                }
            }
        } //投影关联类型，不实例化投影
        if (supposed instanceof UNKNOWN) {
            return this;
        } else {
            Return result = this.copy();
            this.projectConstraints(result, projected, projection, session);
            if (!(this.supposed instanceof UNKNOWN) && (!result.max().is(result.supposed) || !result.supposed.is(result.min()))) {
                ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node()+CONSTANTS.MISMATCH_STR + this.supposed);
            }
            return result;
        }
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
        if (this.supposed == null && declaredToBe != null) {
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
        return super.inferred() && this.supposed.inferred();
    }

//    @Override
//    public String toString() {
//        return (this.supposed instanceof UNKNOWN) ? super.toString(): this.supposed.toString();
//    }

    @Override
    public Outline guess() {
//        return (this.supposed instanceof UNKNOWN) ? super.guess() :
//                (this.supposed instanceof Projectable ? ((Projectable) this.supposed).guess() : this.supposed);
        Outline outline = this.supposed instanceof Projectable ? ((Projectable) this.supposed).guess() : this.supposed;
        return (outline instanceof UNKNOWN)?this.declaredToBe():outline;
    }
}
