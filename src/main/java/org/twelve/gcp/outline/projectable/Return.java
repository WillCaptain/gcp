package org.twelve.gcp.outline.projectable;

import lombok.Setter;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.Map;

import static org.twelve.gcp.common.Tool.cast;

public class Return extends Genericable<Return, Node> implements Returnable {
    @Setter
//    @Getter
    private Long argument;

    private Outline supposed;

    private Return(Node node, AST ast, Outline declared) {
        super(node, ast, declared);
        this.supposed = ast.unknown();
    }

    public static Returnable from(Node node, AST ast, Outline declared) {
        if (declared instanceof Returnable) return cast(declared);
        return new Return(node, ast, declared);
    }

    public static Returnable from(Node node, Outline declared) {
//        if (declared instanceof Returnable) return cast(declared);
//        return new Return(node, node.ast(), declared);
        return from(node, node.ast(), declared);
    }

    public static Returnable from(Node node) {
        return from(node, node.ast().Any);
    }

    public static Returnable from(AST ast, Outline declared) {
        return from(null, ast, declared);
    }

    public static Returnable from(AST ast) {
        return from(ast, ast.Any);
    }

    public boolean addReturn(Outline returns) {
        if (!(returns instanceof IGNORE) && !returns.is(this.declaredToBe)) {
            GCPErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH);
            return false;
        }
        if (supposed instanceof UNKNOWN || (supposed instanceof NOTHING)) {
            supposed = returns;
        } else {
            if(!(returns instanceof NOTHING)) {
                supposed = Option.from(this.node, supposed, returns);
            }
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
    public boolean tryYouAreMe(Outline another) {
        if(this.supposed instanceof UNKNOWN || this.supposed instanceof NOTHING){
            return super.tryYouAreMe(another);
        }else{
            return another.is(this.supposed);
        }
    }

    @Override
    public boolean containsUnknown() {
        return super.containsUnknown() || this.supposed.containsUnknown();
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
        if (this.argument == projected.id()) {
            Return result = this.copy(session.copiedCache());
            this.projectConstraints(result, projected, projection, session);
            if (result.supposedToBe() instanceof UNKNOWN && this.ast().asf().isLastInfer()) {//extension methods doesn't have supposed type
                return result.min();
            }
            if (result.supposedToBe() instanceof NOTHING) {//in higher function, don't return nothing
                return result;
            } else {
                return result.supposedToBe();
            }
        }
        //it is an irrelevant projection
        if (supposedToBe() instanceof UNKNOWN) {
            return this;
        } else {
            Return result = this.copy(session.copiedCache());
            this.projectConstraints(result, projected, projection, session);
            if (!(this.supposedToBe() instanceof UNKNOWN) && (!result.max().is(result.supposedToBe()) || !result.supposedToBe().is(result.min()))) {
                GCPErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this.supposed);
            }
            return result;
        }
    }

    @Override
    protected void projectConstraints(Genericable<?, ?> me, Projectable projected, Outline projection, ProjectSession session) {
        super.projectConstraints(me, projected, projection, session);
        Outline outline = tryProject(((Return) me).supposed, projected, projection, session);
//        ((Return) me).supposed = outline instanceof Projectable ? ((Projectable) outline).guess():outline;
        ((Return) me).supposed = outline;
    }

    @Override
    public boolean equals(Outline another) {
        if (!(another instanceof Return)) return false;
        return this.supposed.equals(((Return) another).supposed);
    }

    @Override
    public Return copy() {
        Return copied = super.copy();
        copied.supposed = this.supposed.copy();
        copied.argument = this.argument;
        return copied;
    }

    @Override
    public Return copy(Map<Outline, Outline> cache) {
        if (cache.containsKey(this)) return cast(cache.get(this));
        Return copied = super.copy(cache);
        copied.supposed = this.supposed.copy(cache);
        copied.argument = this.argument;
        return copied;
    }


//    public void replaceIgnores() {
//        if (this.supposed == Ignore) {
//            this.supposed = Unit;
//        } else {
//            if (!(supposed instanceof Option)) return;
//            Option option = cast(supposed);
//            option.options().removeIf(o -> o == Ignore);
//        }
//    }

    public Outline supposedToBe() {
        if (this.supposed == this.ast().Nothing && declaredToBe != this.ast().Any) {
            return declaredToBe;
        } else {
            return this.supposed;
        }
    }

    @Override
    protected Return createNew() {
        return new Return(this.node, this.ast(), this.declaredToBe);
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
        if (ret.equals("`null`")) {
            ret = "?" + this.id;
        }
        return ret;
    }
}
