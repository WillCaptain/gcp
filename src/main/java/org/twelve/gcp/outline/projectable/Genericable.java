package org.twelve.gcp.outline.projectable;

import lombok.NonNull;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.common.CONSTANTS;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.outline.OutlineWrapper;
import org.twelve.gcp.outline.adt.*;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.builtin.UNKNOWN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.projectable.ConstraintDirection.DOWN;
import static org.twelve.gcp.outline.projectable.ConstraintDirection.UP;

public abstract class Genericable<G extends Genericable, N extends Node> implements Generalizable, OperateAble<N> {
    private final AST ast;
    protected long id;
    protected final N node;

    //extend_to_be(out) <: projection <: declared_to_be <: (in)has_to_be (out)<:(in)defined_to_be
    //x:Number => x.declaredToBe = Number
    protected Outline declaredToBe;

    //x = 100; => x.extend_to_be = Number
    protected Outline extendToBe;
    //var y=100; y=x; => x.has_to_be = Number
    protected Outline hasToBe;
    //x(a) => x.definedToBe = generic->generic
    protected Outline definedToBe;

    protected Genericable(N node, AST ast, Outline declaredToBe) {
        this.node = node;
        this.ast = ast;
        this.id = ast.Counter.getAndIncrement();
        this.extendToBe = ast.Nothing;
        this.hasToBe = ast.Any;
        this.definedToBe = ast.Any;

        this.declaredToBe = (declaredToBe == null || (declaredToBe instanceof UNKNOWN)) ? ast.Any : declaredToBe;
        if (this.declaredToBe instanceof Poly) {
            this.extendToBe = this.declaredToBe.copy();//poly is declared确定了poly必须得到所有可能类型
            this.hasToBe = Poly.create(this.ast());//declared is poly确定了空Poly为最泛化基类
        }
        if (this.declaredToBe instanceof Option) {
            this.extendToBe = Option.from(this.node());//option is declared确定了空option为最泛化子类
            this.hasToBe = this.declaredToBe.copy();//declared is option确定了poly必须得到所有可能类型
        }
    }

    public Genericable(@NonNull N node, Outline declaredToBe) {
        this(node, node.ast(), declaredToBe);
    }

    public Genericable(@NonNull N node) {
        this(node, node.ast().Any);
    }

    public Genericable(AST ast, Outline declaredToBe) {
        this(null, ast, declaredToBe);
    }

    public Genericable(AST ast) {
        this(ast, ast.Any);
    }

    @Override
    public long id() {
        return this.id;
    }

    @Override
    public AST ast() {
        return this.ast;
    }

    /**
     * add constraint for extend, hasto or defined
     *
     * @param target
     * @param constraint
     * @return
     */
    private Outline addDownConstraint(Outline target, Outline constraint) {
        //first time
        if (target instanceof ANY) {
            return constraint;
        }
        //merge entity
        if (target instanceof Entity && constraint instanceof Entity) {
            return ((Entity) target).produce((Entity) constraint);
        }
        //add generic
        if (constraint instanceof Genericable<?, ?>) {
            return new Constraints(ast(), target, cast(constraint), DOWN);
        }
        //return son
        if (target instanceof Constraints) {
            ((Constraints) target).merge(constraint);
        } else {
            if (constraint.is(target)) {
                return constraint;
            }
            if (target.is(constraint)) {
                return target;
            }
            ErrorReporter.report(this.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, constraint.toString() + " doesn't match constraints");
        }
        return target;
    }

    private Outline addUpConstraint(Outline target, Outline constraint) {
        if (target instanceof NOTHING) {
            return constraint;
        }
        if (constraint instanceof Generalizable) {
            return new Constraints(ast(), target, cast(constraint), UP);
        }
        if (target instanceof Constraints) {
            ((Constraints) target).merge(constraint);
        } else {
            if (constraint.is(target)) {
                return target;
            }
            if (target.is(constraint)) {
                return constraint;
            }
            ErrorReporter.report(this.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, constraint.toString() + " doesn't match constraints");
        }
        return target;
    }

    public void addExtendToBe(Outline outline) {
        if (outline instanceof NOTHING) return;
        //find down stream maximum constraint
        Outline downConstraint = this.declaredToBe == ast().Any ? this.hasToBe : declaredToBe;
        downConstraint = downConstraint == ast().Any ? this.definedToBe : downConstraint;
//        if (outline.equals(downConstraint)) {
//            return;
//        }

        if (!outline.is(downConstraint)) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + downConstraint);
            return;
        }

        this.extendToBe = this.addUpConstraint(this.extendToBe, outline);
    }

    private void addDeclaredToBe(Outline declared) {
        if (declared instanceof ANY) return;
        Outline upConstraint = this.extendToBe();
        Outline downConstraint = this.hasToBe == ast().Any ? this.definedToBe : this.hasToBe;

        //find down stream maximum constraint
        if (!declared.is(downConstraint)) {
            ErrorReporter.report(declared.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, declared.node() + CONSTANTS.MISMATCH_STR + downConstraint);
            return;
        }

        if (!upConstraint.is(declared)) {
            ErrorReporter.report(declared.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, declared.node() + CONSTANTS.MISMATCH_STR + upConstraint);
            return;
        }
        this.declaredToBe = this.addDownConstraint(this.declaredToBe, declared);
    }

    public void addHasToBe(Outline outline) {
        if (outline instanceof ANY) return;
        Outline upConstraint = this.declaredToBe == ast().Any ? this.extendToBe : this.declaredToBe;
        Outline downConstraint = this.definedToBe;

//        if (outline.equals(downConstraint)) return;  todo
//        if (upConstraint.equals(outline)) return;

        //find down stream maximum constraint
        if (!outline.is(downConstraint)) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + downConstraint);
            return;
        }

        if (!upConstraint.is(outline)) {
            ErrorReporter.report(outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + upConstraint);
            return;
        }

        this.hasToBe = this.addDownConstraint(this.hasToBe, outline);
    }

    @Override
    public boolean addDefinedToBe(Outline outline) {
        if (outline instanceof ANY) return true;
        //find up stream minimum constraint
        Outline upConstraint = this.hasToBe == ast().Any ? this.declaredToBe : this.hasToBe;
        upConstraint = upConstraint == ast().Any ? this.extendToBe : upConstraint;

        //is chain不满足，退出
        if (!upConstraint.is(outline)) {
            ErrorReporter.report(outline.ast(),outline.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.node() + CONSTANTS.MISMATCH_STR + upConstraint);
            return false;
        }

        this.definedToBe = this.addDownConstraint(this.definedToBe, outline);
        return true;
    }

    public Outline max() {
        if (this.extendToBe instanceof Option) {
            final Option origin = cast(this.extendToBe);
            return new Option(origin.node(), this.ast(), origin.options().toArray(Outline[]::new)) {
                @Override
                public boolean tryIamYou(Outline another) {
                    return this.options.stream().anyMatch(o -> o.is(another));
                }
            };
        } else {
            return this.extendToBe;
        }
    }

    public Outline min() {
        if (this.declaredToBe != ast().Any) return this.declaredToBe;
        if (this.hasToBe != ast().Any) return this.hasToBe;
        return this.definedToBe;
    }

    @Override
    public G copy() {
        G copied = this.createNew();
        copied.extendToBe = this.extendToBe.copy();
        copied.hasToBe = this.hasToBe.copy();
        copied.definedToBe = this.definedToBe.copy();
        copied.id = this.id;
        return copied;
    }

    @Override
    public G copy(Map<Long, Outline> cache) {
        G copied = cast(cache.get(this.id()));
        if (copied == null) {
            copied = this.createNew();
            copied.extendToBe = this.extendToBe.copy(cache);
            copied.hasToBe = this.hasToBe.copy(cache);
            copied.definedToBe = this.definedToBe.copy(cache);
            copied.declaredToBe = this.declaredToBe.copy(cache);
            copied.id = this.id;
            cache.put(this.id(), copied);
        }
        return copied;
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
        if (this.emptyConstraint()) return true;
        if (another instanceof Genericable<?, ?>) {
            Genericable<?, ?> g = cast(another);
            return (g.max().is(this.max()) || this.max() == ast().Nothing) && (this.min().is(g.min()) || this.min() == ast().Any);
        } else {
            if (this.min() instanceof ANY) {
                return this.max().is(another);
            } else {
                return this.min().is(another);
            }
        }
    }

    @Override
    public boolean tryYouAreMe(Outline another) {
        if (this.emptyConstraint()) return true;
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
        if (projection instanceof DictOrArray<?>) {
            return this.projectArray(cast(projection), session);
        }
        return this.projectOutline(projection, session);
    }

    private Outline projectArray(DictOrArray<?> projection, ProjectSession session) {
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
            Returnable b_ = Return.from(d_.node(), d_.ast(), b.project(b, d_, session));
            b_.addReturn(d_);
            //project argument again to make sure the cached projection is fetched
            if (a_ instanceof Projectable) {
                a_ = ((Projectable) a_).project(projected, projection, session);
            }
            //return a_ -> b_
            if(projection.node()==null){
                return FirstOrderFunction.from(projection.ast(), Generic.from(a_.node(), a_.ast(), a_), b_);
            }else {
                return FirstOrderFunction.from(cast(projection.node()), Generic.from(a_.node(), a_.ast(), a_), b_);
            }
        } else {
            ErrorReporter.report(this.node, GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + "between " +
                    b.max() + " and " + b.min());
            return this.guess();
        }


    }

    private Outline projectOutline(Outline projection, ProjectSession session) {
        if (this.definedToBe instanceof Constrainable) {
            ((Constrainable) this.definedToBe).addExtendToBe(projection);
        }
        if (this.hasToBe instanceof Constrainable) {
            ((Constrainable) this.hasToBe).addExtendToBe(projection);
        }
        if (this.declaredToBe instanceof Constrainable) {
            ((Constrainable) this.declaredToBe).addExtendToBe(projection);
        }
        if (this.extendToBe instanceof Constrainable) {
//            if (this.max() instanceof Option) {
//                return ((Option) this.max()).project((Projectable) this.max(), projection, session);
//            } else {
            ((Constrainable) this.extendToBe).addHasToBe(projection);
//            }
        }
        return projection;
    }

    /**
     * genericable(a) project function(b)
     * projection each constraint in genericable(a) with function(b), they will be infected by each other
     *
     * @param projection
     * @param session
     * @return return copy of function(b) infected by genericable(a) constraints
     */
    private Outline projectFunction(FirstOrderFunction projection, ProjectSession session) {
        //project defined
        Outline outline = projectFunctionConstraint(this.definedToBe, projection, session);
        //project has to
        outline = projectFunctionConstraint(this.hasToBe, outline, session);
        //project declared
        outline = projectFunctionConstraint(this.declaredToBe, outline, session);
        //extend to project
        if (this.extendToBe instanceof FirstOrderFunction) {
            Function<?, ?> extProjected = cast(outline);
            FirstOrderFunction extProjection = cast(this.extendToBe);
            session.disable(s -> this.projectLambda(extProjected, extProjection, s));
        }
        if (this.extendToBe instanceof Constraints) {
            Function<?, ?> extProjected = cast(outline);
            Constraints extProjection = cast(this.extendToBe);

            for (Outline constraint : extProjection.constraints()) {
                if (constraint instanceof Projectable) {
                    session.disable(s -> ((Projectable) constraint).project(cast(constraint), extProjection, session));
                }
                if (constraint instanceof Constrainable) {
                    ((Constrainable) constraint).addHasToBe(constraint);
                }
            }
        }

        if (outline.is(this)) {
            return outline;
        } else {
            ErrorReporter.report(projection.ast(), projection.node(),GCPErrCode.OUTLINE_MISMATCH, projection.node() + CONSTANTS.MISMATCH_STR + this);
            return this.guess();
        }
    }

    private Outline projectFunctionConstraint(Outline projected, Outline projection, ProjectSession session) {
        if (!(projected instanceof ANY)) {
            if (projected instanceof Function) {
                return this.projectLambda(cast(projected), cast(projection), session);
            }
            if (projected instanceof Projectable) {
                ((Projectable) projected).project(cast(projected), projection, session);
            }
        }
        return projection;
    }

    /**
     * genericable(a) project entity(b)
     * project entity(b) to each constraint of genericable(a) except extend
     * project extend to entity(b)
     *
     * @param projection entity(b)
     * @param session    projection cache
     * @return infected copy of entity(b)
     */
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
//            this.extendToBe = this.extendToBe.copy();
            ((Projectable) outline).project(cast(outline), cast(this.extendToBe), session);
        }
        if (outline.is(this)) {
            return outline;
        } else {
            ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL, projection.node() + CONSTANTS.MISMATCH_STR + this);
            return this.guess();
        }
    }


    /**
     * genericalbe(a)  project genericable(b)
     *
     * @param projection genericable(b)
     * @param session    save cacehed projection
     * @return copy of genericable(b) contains all constraints from genericable(a)
     */
    private Outline projectGeneric(Genericable<?, ?> projection, ProjectSession session) {
        if (this.emptyConstraint()) return projection;
        Outline max = projection.max();
        Outline min = projection.min();
        Genericable<?, ?> copied = projection.copy();
        Outline extend =this.extendToBe();
        if(this.extendToBe() instanceof Projectable && !(max instanceof NOTHING) ){
            extend = ((Projectable) this.extendToBe()).project(cast(this.extendToBe()),max,session);
        }
        copied.addExtendToBe(extend);
//        copied.addExtendToBe(this.extendToBe());
        copied.addDeclaredToBe(this.declaredToBe());
        copied.addHasToBe(this.hasToBe());
        Outline defined = this.definedToBe();
        if(this.definedToBe() instanceof  Projectable && !(min instanceof ANY)){
            defined = ((Projectable) this.definedToBe()).project(cast(this.definedToBe()),min,session);
        }
        copied.addDefinedToBe(defined);
//        copied.addDefinedToBe(this.definedToBe());

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
            if (next.min() != ast().Any) {
                guessed = next.min();
            } else {
                if (next.max() != ast().Nothing) {
                    guessed = next.max();
                } else {
                    guessed = ast().Any;
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
        me.declaredToBe = tryProject(me.declaredToBe, projected, projection, session);
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

enum ConstraintDirection {
    UP, DOWN
}

class Constraints implements Projectable, Constrainable {
    private final List<Outline> constraints = new ArrayList<>();
    private final ConstraintDirection direction;
    private final AST ast;

    public Constraints(AST ast, Outline c1, Genericable<?, ?> c2, ConstraintDirection direction) {
        this.ast = ast;
        if (c1 instanceof Constraints) {
            for (Outline constraint : ((Constraints) c1).constraints) {
                this.addConstraint(constraint);
            }
//            this.constraints.addAll(((Constraints) c1).constraints);
        } else {
            this.addConstraint(c1);
//            this.constraints.add(c1);
        }
        this.addConstraint(c2);
//        if (c2 instanceof Constraints) {
//            this.constraints.addAll(((Constraints) c2).constraints);
//        } else {
//            this.constraints.add(c2);
//        }
        this.direction = direction;
    }

    private void addConstraint(Outline another) {
        if (this.constraints.isEmpty()) {
            this.constraints.add(another);
            return;
        }
        for (Outline constraint : this.constraints) {
            if (constraint.is(another) || another.is(constraint)) {
                if (constraint instanceof Generalizable || another instanceof Generalizable) {
                    this.constraints.add(another);
                    return;
                }
                //only leave root
                if (constraint.is(another) && direction == DOWN) {
                    this.constraints.remove(constraint);
                    this.constraints.add(another);
                    return;
                }
                //only leave son
                if (another.is(constraint) && direction == UP) {
                    this.constraints.remove(constraint);
                    this.constraints.add(another);
                    return;
                }
            }
        }
        ErrorReporter.report(another.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, another + " doesn't match constraints");
    }

    private Constraints(Constraints another) {
        this.ast = another.ast;
        this.direction = another.direction;
        this.constraints.addAll(another.constraints);
    }

    @Override
    public Outline doProject(Projectable projected, Outline projection, ProjectSession session) {
        Constraints cs = this.copy();
        cs.constraints.clear();
        for (Outline constraint : this.constraints) {
            Outline p = constraint;
            if (constraint instanceof Projectable) {
                p = ((Projectable) constraint).project(projected, projection, session);
            }
            if ((direction == UP) && p.is(projection) || (direction == DOWN && projection.is(p))) {
                cs.addConstraint(p);
            } else {
                ErrorReporter.report(projection.node(), GCPErrCode.PROJECT_FAIL);
            }
        }
        return cs.constraints.size() == 1 ? constraints.getFirst() : cs;
    }

    @Override
    public Constraints copy() {
        return new Constraints(this);
    }

    @Override
    public Outline guess() {
        Constraints cs = this.copy();
        cs.constraints.clear();
        for (Outline constraint : this.constraints) {
            if (constraint instanceof Projectable) {
                cs.constraints.add(((Projectable) constraint).guess());
            } else {
                cs.constraints.add(constraint);
            }
        }
        return cs;
    }

    @Override
    public boolean emptyConstraint() {
        return this.constraints.stream().anyMatch(c -> c instanceof Projectable && ((Projectable) c).emptyConstraint());
    }

    @Override
    public boolean containsGeneric() {
        return true;
    }

    @Override
    public AST ast() {
        return this.ast;
    }

    @Override
    public Node node() {
        return null;
    }

    @Override
    public long id() {
        return -1;
    }

    public void merge(Outline outline) {
        Optional<Outline> maybe = this.constraints.stream().filter(c -> !(c instanceof Generalizable)).findFirst();
        if (maybe.isPresent()) {
            //interact entities
            if (maybe.get() instanceof Entity && outline instanceof Entity && this.direction == DOWN) {
                this.constraints.remove(maybe.get());
                this.constraints.add(((Entity) maybe.get()).produce((Entity) outline));
                return;
            }
            //return son
            if (maybe.get().is(outline)) {
                if (this.direction == DOWN) {
                    return;
                } else {
                    this.constraints.remove(maybe.get());
                    constraints.add(outline);
                    return;
                }
            }
            if (outline.is(maybe.get())) {
                if (this.direction == UP) {
                    return;
                } else {
                    this.constraints.remove(maybe.get());
                    constraints.add(outline);
                    return;
                }
            }
            ErrorReporter.report(this.node(), GCPErrCode.CONSTRUCT_CONSTRAINTS_FAIL, outline.toString() + " doesn't match constraints");
        } else {
            this.constraints.add(outline);
        }
    }

    public List<Outline> constraints() {
        return this.constraints;
    }

    @Override
    public boolean addDefinedToBe(Outline defined) {
        this.constraints.stream().filter(c -> c instanceof Constrainable)
                .forEach(c -> ((Constrainable) c).addDefinedToBe(defined));
        return true;
    }

    @Override
    public void addExtendToBe(Outline extend) {
        this.constraints.stream().filter(c -> c instanceof Constrainable)
                .forEach(c -> ((Constrainable) c).addExtendToBe(extend));
    }

    @Override
    public void addHasToBe(Outline hasTo) {
        this.constraints.stream().filter(c -> c instanceof Constrainable)
                .forEach(c -> ((Constrainable) c).addHasToBe(hasTo));
    }

    @Override
    public String toString() {
        return "?" + this.constraints.stream().map(Object::toString).collect(Collectors.joining(",")) + "?";
    }
}