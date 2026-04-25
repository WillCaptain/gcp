package org.twelve.gcp.outline.projectable;

import lombok.Setter;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.decorators.OutlineWrapper;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.Epsilon;
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

    /**
     * When an external context imposes a lower-bound constraint on this Return
     * (e.g. {@code Return(g).definedToBe = Number} from {@code g(x) - 1}),
     * propagate that constraint as a {@code hasToBe} on the {@code supposed} return
     * expression. This lets {@code Generic(age)} in {@code g = y -> y.age} inherit
     * the {@code Number} requirement so that later projection with a mismatched
     * concrete type (e.g. {@code String}) is detected via
     * {@link Genericable#addExtendToBe}.
     */
    private void propagateToSupposed(Outline outline) {
        if (outline == null || outline instanceof ANY || outline instanceof NOTHING) return;
        if (supposed instanceof UNKNOWN || supposed instanceof NOTHING) return;
        if (supposed instanceof Constrainable constrainable) {
            constrainable.addHasToBe(outline);
        }
    }

    @Override
    public boolean addDefinedToBe(Outline outline) {
        boolean result = super.addDefinedToBe(outline);
        if (result) propagateToSupposed(outline);
        return result;
    }

    @Override
    public void addHasToBe(Outline outline) {
        super.addHasToBe(outline);
        propagateToSupposed(outline);
    }

    public boolean addReturn(Outline returns) {
        if (returns instanceof Epsilon) return true;
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
        Outline result = super.projectMySelf(projection, session);
        if(result instanceof Genericable<?,?> && !(this.supposed instanceof UNKNOWN)){
            ((Genericable<?, ?>) result).addHasToBe(this.supposed);
        }
        return result;
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
                // When a HOF return placeholder (supposed=NOTHING) is projected with a selector function
                // whose return is a member-access (AccessorGeneric), replace the placeholder with that
                // accessor. This wires the result of sel(entity) into the pred HOF argument so that
                // structural constraints (e.g. {points}) can later propagate back through fix #3.
                // Narrowed to AccessorGeneric returns only to avoid affecting arithmetic HOFs like y(x)
                // where the projection is x->x+5 (returns INTEGER, not a member accessor).
                if (projection instanceof FirstOrderFunction) {
                    Outline funcReturn = ((FirstOrderFunction) projection).returns().supposedToBe();
                    if (funcReturn instanceof AccessorGeneric) {
                        return funcReturn;
                    }
                }
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
            // ANY supposed return type means "unconstrained" — skip the consistency check to
            // avoid false-positive "mismatch with any" errors on valid higher-order projections.
            // Also skip when the projection is a function with unresolved (NOTHING) return type:
            // this indicates incomplete HOF inference (e.g. Church numerals) and cannot be
            // reliably validated against formal constraints — reporting here would be a false positive.
            boolean projectionReturnUnresolved = projection instanceof Function<?, ?>
                    && ((Function<?, ?>) projection).returns().supposedToBe() instanceof NOTHING;
            if (!(this.supposedToBe() instanceof UNKNOWN) && !(this.supposedToBe() instanceof ANY)
                    && !projectionReturnUnresolved
                    && (!result.max().is(result.supposedToBe()) || !result.supposedToBe().is(result.min()))) {
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
    public void updateThis(org.twelve.gcp.outline.adt.ProductADT me) {
        super.updateThis(me);
        if (this.supposed != null) {
            this.supposed.updateThis(me);
        }
    }

    @Override
    public boolean equals(Outline another) {
        return this.guardedEquals(another, () -> {
            if (!(another instanceof Return)) return false;
            return this.supposed.equals(((Return) another).supposed);
        });
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
        if (this.supposed == this.ast().Nothing && !(declaredToBe instanceof Epsilon)) {
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
        if (!super.inferred()) return false;
        Outline effective = this.supposedToBe();
        // When supposed is UNKNOWN but declaredToBe provides a concrete declared type
        // (e.g. a generic function member that is declared but never called at a usage
        // site), fall back to the declared type so that Entity.inferred() doesn't
        // incorrectly return false for structurally complete entities.
        if (effective instanceof UNKNOWN && !(declaredToBe instanceof Epsilon)) {
            return declaredToBe.inferred();
        }
        return effective.inferred();
    }

    @Override
    public Outline guess() {
        return this.guardedGuess(() -> this, () -> {
            Outline outline = this.supposed instanceof Projectable ? ((Projectable) this.supposed).guess() : this.supposed;
            outline = (outline instanceof UNKNOWN || outline instanceof NOTHING) ? this.min() : outline;
            return (outline instanceof ANY) ? this.max() : outline;
        });
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
    public Outline eventual() {
        if(this.containsLazyAble()) {
            Return ret = cast(super.eventual());
            ret.supposed = this.supposed.eventual();
            return ret;
        }else{
            return super.eventual();
        }
    }

    @Override
    public boolean containsLazyAble() {
        return super.containsLazyAble() || (this.supposed != null && this.supposed.containsLazyAble());
    }

    @Override
    public String toString() {
        return this.guardedToString("...", () -> {
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
        });
    }
}
