package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.common.Pair;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.ANY;
import org.twelve.gcp.outline.builtin.NOTHING;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public abstract class Genericable<G extends Genericable, N extends Node> implements Generalizable, OperateAble<N> {
    protected long id;
    protected final N node;

    //extend_to_be(out) <: projection <: declared_to_be <: (in)has_to_be (out)<:(in)defined_to_be
    //x:Number => x.declaredToBe = Number
    protected Outline declaredToBe;

    //x = 100; => x.extend_to_be = Number
    protected Outline extendToBe = Nothing;
    //var y=100; y=x; => x.has_to_be = Number
    protected Outline hasToBe = Any;
    //x(a) => x.definedToBe = generic->generic
    protected Outline definedToBe = Any;

    public Genericable(N node, Outline declaredToBe) {
        this.node = node;
        this.id = Counter.getAndIncrement();

        this.declaredToBe = (declaredToBe == null || (declaredToBe instanceof UNKNOWN)) ? Any : declaredToBe;
        if (this.declaredToBe instanceof Poly) {
            this.extendToBe = this.declaredToBe.copy();//poly is declared确定了poly必须得到所有可能类型
            this.hasToBe = Poly.create();//declared is poly确定了空Poly为最泛化基类
        }
        if (this.declaredToBe instanceof Option) {
            this.extendToBe = Option.from(this.node());//option is declared确定了空option为最泛化子类
            this.hasToBe = this.declaredToBe.copy();//declared is option确定了poly必须得到所有可能类型
        }
    }

    public Genericable(N node) {
        this(node, Any);
    }

    @Override
    public long id() {
        return this.id;
    }

    public void addExtendToBe(Outline outline) {
        if (outline instanceof NOTHING) return;
        //find down stream maximum constraint
        Outline downConstraint = this.declaredToBe == Any ? this.hasToBe : declaredToBe;
        downConstraint = downConstraint == Any ? this.definedToBe : downConstraint;
        if (outline.equals(downConstraint)) return;

        if (!outline.is(downConstraint)) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + downConstraint);
            return;
        }


        if (this.extendToBe instanceof Poly) {
            ((Poly) this.extendToBe).sum(outline, true);
            return;
        }
        //留下基类
        if (this.extendToBe.is(outline)) {
            this.extendToBe = outline;
            return;
        }
        if (outline.is(this.extendToBe)) {
            return;
        }
        ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL);

    }

    private void addDeclaredToBe(Outline declared) {
        this.declaredToBe = declared;
    }

    public void addHasToBe(Outline outline) {
        if (outline instanceof ANY) return;
        Outline upConstraint = this.declaredToBe == Any ? this.extendToBe : this.declaredToBe;
        Outline downConstraint = this.definedToBe;

        if (outline.equals(downConstraint)) return;
        if (upConstraint.equals(outline)) return;

        //find down stream maximum constraint
        if (!outline.is(downConstraint)) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + downConstraint);
            return;
        }

        if (!upConstraint.is(outline)) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + upConstraint);
            return;
        }

        if (this.hasToBe instanceof Poly) {
            ((Poly) this.hasToBe).sum(outline, true);
            return;
        }

        if (outline.is(this.hasToBe)) {
            this.hasToBe = outline;
            return;
        }

        if (!this.hasToBe.is((outline))) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + this.hasToBe);
        }
    }

    public void addDefinedToBe(Outline outline) {
        if (outline instanceof ANY) return;
        //find up stream minimum constraint
        Outline upConstraint = this.hasToBe == Any ? this.declaredToBe : this.hasToBe;
        upConstraint = upConstraint == Any ? this.extendToBe : upConstraint;

        //is chain不满足，退出
        if (!upConstraint.is(outline)) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + upConstraint);
            return;
        }

        //第一次赋值，直接赋值并退出
        if (this.definedToBe == Any) {
            this.definedToBe = outline;
            return;
        }

        //得子类
        if (this.definedToBe.is(outline) || outline.is(this.definedToBe)) {
//            if (this.definedToBe.is(outline)) this.definedToBe = outline;
            if (outline.is(this.definedToBe)) this.definedToBe = outline;
            return;
        }

        //非第一次赋值，先满足基础类型的相互is关系，否则退出
        if (!(outline.maybe(this) || this.maybe(outline))) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL,
                    outline.node() + CONSTANTS.MISMATCH_STR + this);
            return;
        }
        //finally, should be entity combine
        if (definedToBe instanceof ProductADT) {
            if (outline instanceof ProductADT) {
                this.definedToBe = Entity.produce(this.node, cast(this.definedToBe), cast(outline));
            } else {
                if (this.definedToBe.maybe(outline)) {
                    List<EntityMember> members = ((ProductADT) this.definedToBe).members();
                    this.definedToBe = Entity.from(this.node, cast(outline));
                    ((Entity) this.definedToBe).addMembers(members);
                }
            }
        } else {
            if (outline instanceof ProductADT && outline.maybe(this.definedToBe)) {
                List<EntityMember> members = ((ProductADT) outline).members();
                this.definedToBe = Entity.from(this.node, cast(this.definedToBe));
                ((Entity) this.definedToBe).addMembers(members);
            } else {
                ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL);
            }
        }
    }

    public Outline max() {
        return this.extendToBe;
    }

    public Outline min() {
        if (this.declaredToBe != Any) return this.declaredToBe;
        if (this.hasToBe != Any) return this.hasToBe;
        return this.definedToBe;
    }

    @Override
    public G copy() {
        G result = this.createNew();
        result.extendToBe = this.extendToBe;
        result.hasToBe = this.hasToBe;
        result.definedToBe = this.definedToBe;
        result.id = this.id;
        return result;
    }

    protected abstract G createNew();

    public Outline declaredToBe() {
        return this.declaredToBe;
    }

    public Outline extendToBe() {
        return this.extendToBe;
    }

    public Outline hasToBe() {
        return this.hasToBe;
    }

    public Outline definedToBe() {
        return this.definedToBe;
    }

    public boolean isEmpty() {
        return (this.max() instanceof NOTHING) && (this.min() instanceof ANY);
    }

    @Override
    public boolean tryIamYou(Outline another) {
        if (this.isEmpty()) return true;
        if (another instanceof Genericable<?, ?>) {
            Genericable<?, ?> g = cast(another);
            return (g.max().is(this.max()) || this.max() == Nothing) && (this.min().is(g.min()) || this.min() == Any);
        } else {
            return this.min().is(another);
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (this.isEmpty()) return true;
        return this.max().is(another) && another.is(this.min());
    }

    @Override
    public boolean equals(Outline another) {
        if (!(another instanceof Genericable<?, ?>)) return false;
        Genericable<?, ?> you = cast(another);
        return you.max().equals(this.max()) && you.min().equals(this.min());
    }

    @Override
    public N node() {
        return this.node;
    }

    @Override
    public boolean inferred() {
        return this.hasToBe().inferred() && this.declaredToBe().inferred()
                && this.extendToBe().inferred() && this.definedToBe().inferred();
    }

    protected Outline tryProject(Outline outline, Outline projected, Outline projection, ProjectSession session) {
        if (outline instanceof Projectable) {
            return ((Projectable) outline).project(cast(projected), projection, session);
        } else {
            return outline;
        }
    }

    protected Outline projectMySelf(Outline projection, ProjectSession session) {
        if (!projection.is(this)) {
            ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this.node());
            return this.guess();
        }
        if (projection instanceof Genericable) {
            return this.projectGeneric(cast(projection), session);
        }
        if (projection instanceof FirstOrderFunction) {
            return this.projectFunction(cast(projection), session);
        }
        if (projection instanceof Entity) {
            return this.projectEntity(cast(projection), session);
        }
        if (projection instanceof Array) {
            return this.projectArray(cast(projection), session);
        }
//        if (projection.is(this)) {
        return this.projectOutline(projection, session);
//        } else {
//            ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this.node());
//            return this.guess();
//        }
    }

    private Outline projectArray(Array projection, ProjectSession session) {
        if (this.extendToBe() instanceof Projectable) {
            ((Projectable) this.extendToBe()).project(cast(this.extendToBe()), projection, session);
        }
        if (this.declaredToBe() instanceof Projectable) {
            ((Projectable) this.declaredToBe()).project(cast(this.declaredToBe()), projection, session);
        }
        if (this.hasToBe() instanceof Projectable) {
            ((Projectable) this.hasToBe()).project(cast(this.hasToBe()), projection, session);
        }
        if (this.definedToBe() instanceof Projectable) {
            ((Projectable) this.definedToBe()).project(cast(this.definedToBe()), projection, session);
        }
        return projection;
    }

    protected Outline projectLambda(Function<?, ?> projected, FirstOrderFunction projection, ProjectSession session) {
        /*function:a->b project lambda: c->d
          a_ = c.project(a)
          d_ = d.project(a)
          b_ = b.project(d_)
          return a_ -> b_
         */
        Outline a = projected.argument();
        Returnable b = projected.returns();

        //a_ = c.project(a)
        Outline a_ = projection.argument.project(projection.argument, a, session);
        //d_ = d.project(a)
        Outline d_ = projection.returns.project(projection.argument, a, session);
        //curry project....
        if (b.min() instanceof HigherOrderFunction) {
            if (d_ instanceof FirstOrderFunction) {
                d_ = projectLambda(cast(b.min()), cast(d_), session);
            } else {
                ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + b.min());
                d_ = ((HigherOrderFunction) b.min()).guess();
            }
        }
        //return a_ -> b_
        if (b.max().is(d_) && d_.is(b.min())) {
            //b_ = b.project(d_)
            Returnable b_ = Return.from(b.project(b, d_, session));
            b_.addReturn(d_);
            //project argument again to make sure the cached projection is fetched
            if (a_ instanceof Projectable) {
                a_ = ((Projectable) a_).project(projected, projection, session);
            }
            //return a_ -> b_
            return FirstOrderFunction.from(cast(projection.node()), Generic.from(a_.node(), a_), b_);
        } else {
            ErrorReporter.report(this.node, GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + "between " +
                    b.max() + " and " + b.min());
            return this.guess();
        }


    }

    private Outline projectOutline(Outline projection, ProjectSession session) {
        if (this.definedToBe instanceof Genericable<?, ?>) {
            ((Genericable<?, ?>) this.definedToBe).addExtendToBe(projection);
        }
        if (this.hasToBe instanceof Genericable<?, ?>) {
            ((Genericable<?, ?>) this.hasToBe).addExtendToBe(projection);
        }
        if (this.declaredToBe instanceof Genericable<?, ?>) {
            ((Genericable<?, ?>) this.declaredToBe).addExtendToBe(projection);
        }
        if (this.extendToBe instanceof Genericable<?, ?>) {
            ((Genericable<?, ?>) this.extendToBe).addHasToBe(projection);
        }
        return projection;
    }

    private Outline projectFunction(FirstOrderFunction projection, ProjectSession session) {
        Outline outline = projection;
        if (!(this.definedToBe instanceof ANY)) {
            outline = this.projectLambda(cast(this.definedToBe), projection, session);
        }
        if (!(this.hasToBe instanceof ANY)) {
            if (this.hasToBe instanceof Function) {
                outline = this.projectLambda(cast(this.hasToBe), cast(outline), session);
            }
            if (this.hasToBe instanceof Genericable) {
                ((Genericable<?, ?>) this.hasToBe).projectMySelf(projection, session);
            }
        }
        if (!(this.declaredToBe instanceof ANY)) {
            if (this.declaredToBe instanceof Function) {
                outline = this.projectLambda(cast(this.declaredToBe), cast(outline), session);
            }
            if (this.declaredToBe instanceof Genericable) {
                ((Genericable<?, ?>) this.declaredToBe).projectMySelf(projection, session);
            }
        }
        if (!(this.extendToBe instanceof NOTHING)) {
            this.projectLambda(cast(outline), cast(this.extendToBe), session);
        }

        if (outline.is(this)) {
            return outline;
        } else {
            ErrorReporter.report(projection.node(), GCPErrCode.OUTLINE_MISMATCH, projection.node() + CONSTANTS.MISMATCH_STR + this);
            return this.guess();
        }
    }

    private Outline projectEntity(Entity projection, ProjectSession session) {
        Outline outline = projection;
        if (!(this.definedToBe instanceof ANY)) {
            outline = ((Projectable) this.definedToBe).project(cast(this.definedToBe), projection, session);
        }
        if (!(this.hasToBe instanceof ANY)) {
            outline = ((Projectable) this.hasToBe).project(cast(this.hasToBe), projection, session);
        }
        if (!(this.declaredToBe instanceof ANY)) {
            outline = ((Projectable) this.declaredToBe).project(cast(this.declaredToBe), projection, session);
        }
        if (!(this.extendToBe instanceof NOTHING)) {
            ((Projectable) outline).project(cast(outline), cast(this.definedToBe), session);
        }
        if (outline.is(this)) {
            return outline;
        } else {
            ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this);
            return this.guess();
        }
    }


    private Outline projectGeneric(Genericable<?, ?> projection, ProjectSession session) {
        if (this.isEmpty()) return projection;
        Genericable<?, ?> copied = projection.copy();
        copied.addExtendToBe(this.extendToBe());
        copied.addDeclaredToBe(this.declaredToBe());
        copied.addHasToBe(this.hasToBe());
        copied.addDefinedToBe(this.definedToBe());

        if (copied.is(this)) {
            return copied;
        } else {
            ErrorReporter.report(copied.node, GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this);
            return this.guess();
        }

    }

    @Override
    public Outline guess() {
        Outline guessed = this;
        while (guessed instanceof Genericable) {
            Genericable<?, ?> next = cast(guessed);
            if (next.min() != Any) {
                guessed = next.min();
            } else {
                if (next.max() != Nothing) {
                    guessed = next.max();
                } else {
                    guessed = Any;
                }
            }
            if (guessed instanceof Reference) break;
        }
        return guessed instanceof Projectable ? ((Projectable) guessed).guess() : guessed;
    }

    /*@Override
    public Outline project(Projectable projected, Outline projection, ProjectSession session) {
        if (this.node() == null && !(this.declaredToBe() instanceof ANY)) {//this is outline definition
            if (this.id() == projected.id()) {
                if (projection instanceof Genericable) {
                    ((Genericable<?, ?>) projection).addDeclaredToBe(this.declaredToBe());
                }
                if (!projection.is(this.declaredToBe())) {
                    ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this.declaredToBe());
                }
                return this.declaredToBe();
            }
            if (this.declaredToBe() instanceof Projectable) {
                return ((Projectable) this.declaredToBe()).project(projected, projection, session);
            } else {
                return this.declaredToBe();
            }
        }
        return Projectable.super.project(projected, projection, session);
    }*/

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        //project myself
        if (projected.id() == this.id()) {
            return this.projectMySelf(projection, session);
        } else {//投影靠前的参数
            Genericable<?, ?> result = this.copy();
            projectConstraints(result, projected, projection, session);
            return result;
        }
    }

    protected void projectConstraints(Genericable<?, ?> me, Projectable projected, Outline projection, ProjectSession session) {
        me.extendToBe = tryProject(me.extendToBe, projected, projection, session);
        me.hasToBe = tryProject(me.hasToBe, projected, projection, session);
        me.definedToBe = tryProject(me.definedToBe, projected, projection, session);
    }

    @Override
    public String toString() {
        return "`" + this.guess().toString() + "`";
    }

    @Override
    public Outline project(Reference reference, OutlineWrapper projection) {
        G copied = this.copy();
        Node node = projection.node();
        copied.extendToBe = this.extendToBe.project(reference, projection);
        copied.hasToBe = this.hasToBe.project(reference, projection);
        copied.declaredToBe = this.declaredToBe.project(reference, projection);
        copied.definedToBe = this.definedToBe.project(reference, projection);
        Outline benchMark = copied.definedToBe;
        if (copied.hasToBe instanceof ANY || copied.hasToBe.is(copied.definedToBe)) {
            if (!(copied.hasToBe instanceof ANY)) benchMark = copied.hasToBe;
        } else {
            ErrorReporter.report(node, GCPErrCode.PROJECT_FAIL, copied.hasToBe + " doesn't match " + copied.definedToBe);
        }
        if (copied.declaredToBe instanceof ANY || copied.declaredToBe.is(benchMark)) {
            if (!(copied.declaredToBe instanceof ANY)) benchMark = copied.declaredToBe;
        } else {
            ErrorReporter.report(node, GCPErrCode.PROJECT_FAIL, copied.declaredToBe + " doesn't match " + benchMark);
        }
        if (!copied.extendToBe.is(benchMark)) {
            ErrorReporter.report(node, GCPErrCode.PROJECT_FAIL, copied.extendToBe + " doesn't match " + benchMark);
        }
        return copied;
    }
}
