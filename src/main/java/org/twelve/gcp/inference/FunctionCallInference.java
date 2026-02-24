package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outline.decorators.Lazy;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.projectable.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.*;

/**
 * Type inference for function call nodes.
 *
 * <h2>Core Flow</h2>
 * <ol>
 *   <li>Infer the type of the callee (func).</li>
 *   <li>If func is a Poly (overloaded), select the matching overload.</li>
 *   <li>Project each argument in order, narrowing generic constraints and deriving the return type.</li>
 * </ol>
 *
 * <h2>Two Call Modes</h2>
 * <ul>
 *   <li><b>FOF (First-Order Function) call</b>: the function type is fully known ({@link FirstOrderFunction}).
 *       Generic parameters are instantiated via {@link org.twelve.gcp.outline.projectable.ProjectSession},
 *       yielding a concrete return type.</li>
 *   <li><b>HOF (Higher-Order Function) call</b>: the function type is not yet known ({@link Genericable}).
 *       The function signature is inferred from {@code hasToBe} or {@code definedToBe} constraints,
 *       which are progressively narrowed across inference passes.</li>
 * </ul>
 *
 * <h2>Lazy Deferred Inference</h2>
 * When a call appears inside a function body at a member-access position and the current pass
 * is not the final one, a {@link org.twelve.gcp.outline.decorators.Lazy} placeholder is returned
 * to defer processing and avoid premature convergence or infinite recursion.
 *
 * @author huizi 2025
 */
public class FunctionCallInference implements Inference<FunctionCallNode> {
    @Override
    public Outline infer(FunctionCallNode node, Inferences inferences) {
        AST ast = node.ast();
        // Member-access call inside a function body (e.g. HOF callback): defer on non-final passes to prevent premature convergence
        if (inferences.isLazy() && isInFunction(node) && isInMember(node)) {
            return new Lazy(node, ast.inferences());
        }
        Outline func = node.function().invalidate().infer(inferences);
        if (func == null) {
            GCPErrorReporter.report(node, GCPErrCode.FUNCTION_NOT_DEFINED);
            return ast.Error;
        }
        if (func == ast.Pending) {// recursive call: return Pending and wait for the next inference pass
            return func;
        }

        Outline result = ast.unknown(node);
        // Overloaded function: select the matching version from the Poly union
        if (func instanceof Poly) {
            result = targetOverride(cast(func), node.arguments(), inferences, node);
        } else {
            // Non-overloaded: verify argument match, or force-proceed on the final pass to avoid stalling
            if ((func instanceof Function<?, ?> &&
                    this.matchFunction((Function<?, ?>) func, node.arguments(), inferences, node))
                    || node.ast().asf().isLastInfer()) {
                result = func;
            }
        }
        if (result == ast.unknown(node)) {
            GCPErrorReporter.report(node, GCPErrCode.FUNCTION_NOT_FOUND);
            return result;
        }

        if (node.arguments().isEmpty()) {
            result = project(result);
        } else {
            // Project each argument in order; each projection further narrows the generic constraints
            for (Expression argument : node.arguments()) {
                argument.infer(inferences);
                result = project(result, argument);
            }
        }
        return result.instantiate();
    }

    /**
     * Selects the matching overload from a Poly (overload set) based on the actual arguments.
     *
     * @param overwrite  the overload set
     * @param arguments  the actual argument list
     * @param inferences the inference context
     * @param node       the call node (used for error reporting)
     * @return the matching function type, or UNKNOWN if no overload matches
     */
    private Outline targetOverride(Poly overwrite, List<Expression> arguments, Inferences inferences, FunctionCallNode node) {
        List<Function<?, ?>> fs = overwrite.options().stream().filter(o -> o instanceof Function).map(o -> (Function<?, ?>) o).collect(Collectors.toList());
        for (Function<?, ?> f : fs) {
            if (this.matchFunction(f, arguments, inferences, node)) {
                return f;
            }
        }
        return node.ast().unknown(node);
    }

    /**
     * Checks whether a function type matches the actual argument list (currying supported).
     * For a curried multi-argument function, each argument corresponds to one nested function layer.
     *
     * @param function   the candidate function type
     * @param arguments  the actual argument list
     * @param inferences the inference context
     * @param node       the call node
     * @return {@code true} if all arguments match
     */
    private boolean matchFunction(Function<?, ?> function, List<Expression> arguments, Inferences inferences, FunctionCallNode node) {
        Function<?, ?> f = null;
        for (Expression argument : arguments) {
            if (f == null) {// first argument: use the function type directly
                f = function;
            } else {// n-th argument: use the previous return type as the next function (currying)
                if (function.returns().supposedToBe() instanceof Function) {
                    f = cast(function.returns().supposedToBe());
                } else {
                    return false;
                }
            }
            Outline arg = argument.infer(inferences);
            if (!arg.is(f.argument())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Projects a zero-argument function call (returns the function's return type).
     */
    private Outline project(Outline target) {
        if (target instanceof Genericable) {
            return this.project((Genericable<?, ?>) target, null);
        }
        if (target instanceof Function<?, ?>) {
            return ((Function<?, ?>) target).returns().supposedToBe();
        }

        return target.ast().Error;
    }

    /**
     * Entry point for projecting a function call with arguments.
     * Dispatches to the HOF or FOF projection path based on the function's type.
     */
    private Outline project(Outline target, AbstractNode argument) {
        // HOF (generic function): type not yet fully known; infer via constraints
        if (target instanceof Genericable) {
            return project((Genericable<?, ?>) target, argument);
        }
        // FOF (first-order function): type is known; perform generic parameter instantiation directly
        if (target instanceof FirstOrderFunction) {
            return project((FirstOrderFunction) target, argument);

        }
        GCPErrorReporter.report(argument, GCPErrCode.NOT_A_FUNCTION);
        return argument.ast().Error;
    }

    /**
     * Projects an HOF (Higher-Order Function) call where the function type is not yet fully known.
     * <p>
     * Resolution strategy (in priority order):
     * <ol>
     *   <li>If {@code definedToBe} is already a {@link HigherOrderFunction}, return its return type directly.</li>
     *   <li>If {@code definedToBe} is a Poly, find a {@link HigherOrderFunction} among its options and return its return type.</li>
     *   <li>If {@code hasToBe} holds a concrete function type (propagated by {@link Entity#doProject}
     *       during entity projection), use it to validate the argument type and return its return type.
     *       This is the critical path for method calls defined in Outline types (e.g. {@code agg.avg(...)}).</li>
     *   <li>Fallback: create a virtual {@link HigherOrderFunction} and register it in {@code definedToBe},
     *       leaving further narrowing to subsequent inference passes.</li>
     * </ol>
     *
     * @param generic  the Genericable representing the as-yet-undetermined function
     * @param argument the actual argument node
     * @return the inferred return type
     */
    private Outline project(Genericable<?, ?> generic, AbstractNode argument) {
        if (generic.definedToBe() instanceof HigherOrderFunction) {
            return ((HigherOrderFunction) generic.definedToBe()).returns();
        }
        if (generic.definedToBe() instanceof Poly) {
            Optional<Outline> option = ((Poly) generic.definedToBe()).options().stream().filter(o -> o instanceof HigherOrderFunction).findFirst();
            if (option.isPresent()) {
                return ((HigherOrderFunction) option.get()).returns();
            }
        }
        // hasToBe holds the formal function type propagated by Entity.doProject during entity projection.
        // Used to validate arguments for method calls defined in Outline types (e.g. agg.avg(e->e.age)).
        Outline hasToBe = generic.hasToBe();
        if (!(hasToBe instanceof ANY)
                && hasToBe instanceof Function
                && !(hasToBe instanceof FixFunction)) {
            Function<?, ?> formalFunc = cast(hasToBe);
            if (argument != null) {
                Outline argOutline = argument.outline();
                if (!argOutline.is(formalFunc.argument())) {
                    GCPErrorReporter.report(argument, GCPErrCode.OUTLINE_MISMATCH,
                            argument + " mismatch: expected " + formalFunc.argument() + " but got " + argOutline);
                }
            }
            Outline returnType = formalFunc.returns().supposedToBe();
            if (returnType instanceof UNKNOWN || returnType instanceof NOTHING) {
                return formalFunc.returns();
            }
            return returnType;
        }
        // Fallback: create a virtual HOF definition to provide structural constraints for later passes
        Returnable returns = Return.from(generic.node());
        Outline argOutline = argument == null ? generic.node().ast().Unit : argument.outline();
        HigherOrderFunction defined = new HigherOrderFunction(generic.node(), argOutline, returns);
        generic.addDefinedToBe(defined);
        return returns;
    }

    /**
     * Projects an FOF (First-Order Function) call where the function type is fully known.
     * <p>
     * Uses a {@link ProjectSession} to cache type substitutions for this call, supporting
     * incremental projection of curried multi-argument functions:
     * <ol>
     *   <li>Copy the function type (to avoid mutating the original definition).</li>
     *   <li>Project the argument: inject the actual argument type into the formal parameter's generic constraints.</li>
     *   <li>Back-propagate the projected constraints to the original argument node's type.</li>
     *   <li>Project the return type: derive the concrete return type for this call.</li>
     * </ol>
     *
     * @param function the first-order function with a fully known type
     * @param argument the actual argument node
     * @return the projected return type
     */
    private Outline project(FirstOrderFunction function, AbstractNode argument) {
        ProjectSession session = function.getSession();
        if (session == null) {
            // start a new projection session for this call
            session = new ProjectSession();
        }
        session.copiedCache().clear();
        function = function.copy(session.copiedCache());
        // project argument: inject actual type into the formal parameter's generic constraints
        Outline projectedArg = function.argument().project(function.argument(), argument.outline(), session);
        //change the argument constraints
        if (projectedArg.node() != null && projectedArg.id() == projectedArg.node().outline().id()
                && projectedArg != projectedArg.node().outline()) {
            Genericable<?, ?> origin = cast(projectedArg.node().outline());
            Genericable<?, ?> projected = cast(projectedArg);
            origin.addExtendToBe(projected.extendToBe());
            origin.addHasToBe(projected.declaredToBe());
            origin.addHasToBe(projected.hasToBe());
            origin.addDefinedToBe(projected.definedToBe());
        }
        // project return type: resolve the concrete return type for this call
        Outline result = function.returns().project(function.argument(), projectedArg, session);
        if (result instanceof FirstOrderFunction) {
            ((FirstOrderFunction) result).setSession(session);
        }
        //if this is the final projected, remove generics
        if (result instanceof Option) {
            ((Option) result).options().removeIf(o -> o instanceof Generic);
            if (((Option) result).options().size() == 1) {
                result = ((Option) result).options().getFirst();
            }
        }
        return result;

    }
}
