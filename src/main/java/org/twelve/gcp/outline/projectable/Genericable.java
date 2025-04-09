package org.twelve.gcp.outline.projectable;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.ProductADT;
import org.twelve.gcp.outline.adt.EntityMember;
import org.twelve.gcp.outline.builtin.ANY;

import java.util.List;

import static org.twelve.gcp.common.Tool.cast;

public abstract class Genericable<G extends Genericable, N extends Node> implements Projectable, OperateAble<N> {
    protected long id;
    protected final N node;

    //extend_to_be(out) <: projection <: declared_to_be <: (in)has_to_be (out)<:(in)defined_to_be
    //x:Number => x.declaredToBe = Number
    protected final Outline declaredToBe;

    //x = 100; => x.extend_to_be = Number
    protected Outline extendToBe = Nothing;
    //var y=100; y=x; => x.has_to_be = Number
    protected Outline hasToBe = Any;
    //x(a) => x.definedToBe = generic->generic
    protected Outline definedToBe = Any;
    //if(x is A as y){...}
    protected Outline couldBe = Any;
    private Outline max = Nothing;
    private Outline min = Any;

    public Genericable(N node, Outline declaredToBe) {
        this.node = node;
        this.id = Counter.getAndIncrement();

        this.declaredToBe = declaredToBe == Unknown ? Any : declaredToBe;
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
        //find down stream maximum constraint
        Outline downConstraint = this.declaredToBe == Any ? this.hasToBe : declaredToBe;
        downConstraint = downConstraint == Any ? this.definedToBe : downConstraint;
//        if (downConstraint != null && outline.equals(downConstraint)) return;
        if (outline.equals(downConstraint)) return;

//        if (!(downConstraint == null || outline.is(downConstraint))) {
        if (!outline.is(downConstraint)) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);
            return;
        }


        if (this.extendToBe instanceof Poly) {
            ((Poly) this.extendToBe).sum(outline, null, true);
            return;
        }
        if (this.extendToBe instanceof Option) {
            ((Option) this.extendToBe).sum(outline);
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
        ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);

    }

    public void addHasToBe(Outline outline) {
        Outline upConstraint = this.declaredToBe == Any ? this.extendToBe : this.declaredToBe;
        Outline downConstraint = this.definedToBe;

        if (outline.equals(downConstraint)) return;
        if (upConstraint.equals(outline)) return;

        //find down stream maximum constraint
        if (!outline.is(downConstraint)) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);
            return;
        }

        //find up stream minimum constraint
        if (!upConstraint.is(outline)) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);
            return;
        }

        if (this.hasToBe instanceof Poly) {
            ((Poly) this.hasToBe).sum(outline, null, true);
            return;
        }

        if (outline.is(this.hasToBe)) {
            this.hasToBe = outline;
            return;
        }

        if (!this.hasToBe.is((outline))) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);
        }
    }

    public void addDefinedToBe(Outline outline) {
        //find up stream minimum constraint
        Outline upConstraint = this.hasToBe == Any ? this.declaredToBe : this.hasToBe;
        upConstraint = upConstraint == Any ? this.extendToBe : upConstraint;

        //is chain不满足，退出
        if (!upConstraint.is(outline)) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);
            return;
        }

        //第一次赋值，直接赋值并退出
        if (this.definedToBe == Any) {
            this.definedToBe = outline;
            return;
        }

        //得基类
        if (this.definedToBe.is(outline) || outline.is(this.definedToBe)) {
            if (this.definedToBe.is(outline)) this.definedToBe = outline;
            return;
        }

        //非第一次赋值，先满足基础类型的相互is关系，否则退出
        if (!(outline.maybe(this) || this.maybe(outline))) {
            ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);
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
                ErrorReporter.report(this.node, GCPErrCode.OUTLINE_MISMATCH_IN_GCP);
            }
        }
    }

    protected Outline max() {
        if (this.extendToBe != Nothing) return this.extendToBe;
        else return max;
    }

    protected Outline min() {
        if (this.declaredToBe != Any) return this.declaredToBe;
        if (this.hasToBe != Any) return this.hasToBe;
        if (this.definedToBe != Any) return this.definedToBe;
        return min;
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

    @Override
    public boolean tryIamYou(Outline another) {
        if (another instanceof Generic) {
            Generic g = cast(another);
            return (g.max().is(this.max())||this.max()==Nothing) && (this.min().is(g.min())||this.min()==Any);
        } else {
            return this.min().is(another);
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        return this.max().is(another) && another.is(this.min());
    }

    @Override
    public boolean equals(Outline another) {
        if (!(another instanceof Genericable)) return false;
        Genericable you = cast(another);
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

        if (projection instanceof Genericable) {
            return this.projectGeneric(cast(projection), session);
        }
        if (projection instanceof Function) {
            return this.projectFunction(cast(projection), session);
        }
        if (projection instanceof Entity) {
            return this.projectEntity(cast(projection), session);
        }
        if (projection.is(this)) {
            return this.projectOutline(projection, session);
        } else {
            ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
            return this.guess();
        }
    }

    protected Outline projectLambda(HigherOrderFunction projected, FirstOrderFunction projection, ProjectSession session) {
        Outline arg = projected.argument();
        Return lambdaReturn = projected.returns();

        //F1.arg.project(F2.arg) 验证合法
        Outline argProjected = projection.argument.project(projection.argument, arg, session);
        //F2.return.project(F1.arg)得到投影值projected_return
        Outline returnProjected = projection.returns.project(cast(projection.argument), arg, session);

        if (lambdaReturn.min() instanceof HigherOrderFunction) {
            if(returnProjected instanceof FirstOrderFunction) {
                returnProjected = projectLambda(cast(lambdaReturn.min()), cast(returnProjected), session);
            }else{
                ErrorReporter.report(this.node,GCPErrCode.PROJECT_FAIL);
                returnProjected = ((HigherOrderFunction) lambdaReturn.min()).guess();
            }
        }
        if (lambdaReturn.max().is(returnProjected) && returnProjected.is(lambdaReturn.min())) {
            lambdaReturn.project(lambdaReturn,returnProjected,session);
            Outline result = new FixFunction(projection.node(), argProjected, returnProjected);
            return result;
        }else{
            ErrorReporter.report(this.node,GCPErrCode.PROJECT_FAIL);
            return this.guess();
        }


    }

    private Outline projectOutline(Outline projection, ProjectSession session) {
//        session.addProjection(this, projection);
        return projection;
    }

    private Outline projectFunction(FirstOrderFunction projection, ProjectSession session) {
        Outline outline = projection;
//        if (this.definedToBe instanceof Function) {
        if (this.min() instanceof Function) {
            //G1.project(F1),G1.defined_to_be = F2
//            HigherOrderFunction defined = cast(this.definedToBe);//得到函数定义里HOF的调用形成的函数定义，是一个higher order function
            HigherOrderFunction defined = cast(this.min());//得到函数定义里HOF的调用形成的函数定义，是一个higher order function
            outline = this.projectLambda(defined, projection, session);
//            outline = this.projectLambda(defined.returns, projection, session);
//            session.addProjection(this, outline);
        }
        if (outline.is(this)) {
            return outline;
        } else {
//            return Error;
            ErrorReporter.report(projection.node(), GCPErrCode.OUTLINE_MISMATCH);
            return this.guess();
        }
    }

    private Outline projectEntity(Entity projection, ProjectSession session) {
        Outline outline = projection;
        Outline min = this.min();
        if (min instanceof Entity) {
            outline = ((Entity) min).project(cast(min), projection, session);
//            if (outline instanceof ERROR) return outline;
        }
        if (outline.is(this)) {
            return outline;
        } else {
            ErrorReporter.report(projection.node(), GCPErrCode.OUTLINE_MISMATCH);
            return this.guess();
        }
    }


    private Outline projectGeneric(Genericable projection, ProjectSession session) {
        Genericable copied = projection.copy();
        copied.addMax(this.max());
        copied.addMin(this.min());

//        session.addProjection(this, copied);
        if (copied.is(this)) {
            return copied;
        } else {
            ErrorReporter.report(copied.node, GCPErrCode.OUTLINE_MISMATCH);
            return this.guess();
        }

    }

    @Override
    public Outline guess() {
        Outline guessed = this;
        while (guessed instanceof Genericable) {
            Genericable next = cast(guessed);
            if (next.min() != Any) {
                guessed = next.min();
            } else {
                if (next.max() != Nothing) {
                    guessed = next.max();
                } else {
                    guessed = Any;
                }
            }
        }
        return guessed instanceof Projectable ? ((Projectable) guessed).guess() : guessed;
    }

    private void addMin(Outline min) {
        if (this.min() == Any) {
            this.min = min;
        }
    }

    private void addMax(Outline max) {
        if (this.max() == Nothing) {
            this.max = max;
        }
    }

    @Override
    public Outline project(Projectable projected, Outline projection, ProjectSession session) {
        if (this.node() == null && !(this.declaredToBe() instanceof ANY)) {//this is outline definition
            if (this.id() == projected.id()) {
                if (projection instanceof Genericable) {
                    ((Genericable) projection).addMin(this.declaredToBe());
                }
                if (!projection.is(this.declaredToBe())) {
                    ErrorReporter.report(projection.node(), GCPErrCode.OUTLINE_MISMATCH);
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
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        //project myself
        if (projected.id() == this.id()) {
            //目标已经被投影，直接得到投影值
//            if (session.getProjection(this) != null) {
//                return session.getProjection(this);
//            }
            return this.projectMySelf(projection, session);
        } else {//投影靠前的参数
            Genericable result = this.copy();
            result.extendToBe = tryProject(result.extendToBe, projected, projection, session);
            result.hasToBe = tryProject(result.hasToBe, projected, projection, session);
            result.definedToBe = tryProject(result.definedToBe, projected, projection, session);
            return result;
        }
    }

    @Override
    public String toString() {
//        StringBuilder constraint = new StringBuilder("generic");
//        if (!(this.max() instanceof NOTHING) || !(this.max() instanceof ANY)) {
//            constraint.append("( ");
//            if (!(this.max() instanceof NOTHING)) constraint.append(this.max().toString());
//            if (!(this.min() instanceof ANY))
//                constraint.append((this.max() instanceof NOTHING ? "" : ",") + this.min().toString());
//            constraint.append(")");
//        }
//        return constraint.toString();
        return "`" + this.guess().toString() + "`";
    }
}
