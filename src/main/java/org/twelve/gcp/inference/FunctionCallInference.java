package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.statement.MemberNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.decorators.Lazy;
import org.twelve.gcp.outline.projectable.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.*;

public class FunctionCallInference implements Inference<FunctionCallNode> {
    @Override
    public Outline infer(FunctionCallNode node, Inferences inferences) {
        AST ast = node.ast();
        if(inferences.isLazy() && isInFunction(node) && isInMember(node)) {
            return new Lazy(node,ast.inferences());
        }
        Outline func = node.function().invalidate().infer(inferences);
//        func.toString();
        if (func == null) {
            GCPErrorReporter.report(node, GCPErrCode.FUNCTION_NOT_DEFINED);
            return ast.Error;
        }
        if (func == ast.Pending) {//recursive
            return func;
        }

        Outline result = ast.unknown(node);
        //如果有重载方法
        if (func instanceof Poly) {
            result = targetOverride(cast(func), node.arguments(), inferences, node);
        } else {
            if ((func instanceof Function<?, ?> &&
                    this.matchFunction((Function<?, ?>) func, node.arguments(), inferences, node))
                    || node.ast().asf().isLastInfer()) {
                result = func;
            }

        }
//        if (result == Outline.Unknown && !node.ast().asf().isLastInfer()) {
        if (result == ast.unknown(node)) {
            GCPErrorReporter.report(node, GCPErrCode.FUNCTION_NOT_FOUND);
            return result;
        }

        if (node.arguments().isEmpty()) {
            //result = ((Function<?, ?>) result).returns().supposedToBe();
            result = project(result);
        } else {
            //按顺序投影参数
            for (Expression argument : node.arguments()) {
                argument.infer(inferences);
                result = project(result, argument);
            }
        }
        return result.eventual();
    }

    private Outline targetOverride(Poly overwrite, List<Expression> arguments, Inferences inferences, FunctionCallNode node) {
        List<Function<?, ?>> fs = overwrite.options().stream().filter(o -> o instanceof Function).map(o -> (Function<?, ?>) o).collect(Collectors.toList());
        for (Function<?, ?> f : fs) {
            if (this.matchFunction(f, arguments, inferences, node)) {
                return f;
            }
        }
        return node.ast().unknown(node);
    }

    private boolean matchFunction(Function<?, ?> function, List<Expression> arguments, Inferences inferences, FunctionCallNode node) {
        Function<?, ?> f = null;
        for (Expression argument : arguments) {
            if (f == null) {//匹配第一个参数
                f = function;
            } else {//匹配第n参数
                if (function.returns().supposedToBe() instanceof Function) {
                    f = cast(function.returns().supposedToBe());
                } else {
                    //ErrorReporter.report(node, GCPErrCode.ARGUMENT_MISMATCH);
                    return false;
                }
            }
            Outline arg = argument.infer(inferences);
            if (!arg.is(f.argument())) {
                //ErrorReporter.report(node, GCPErrCode.ARGUMENT_MISMATCH);
                return false;
            }
        }
        return true;
    }

    private Outline project(Outline target) {
        if (target instanceof Genericable) {
//            Returnable returns = Return.from(target.ast());
//            HigherOrderFunction defined = new HigherOrderFunction(target.ast(), target.ast().Unit, returns);
//            ((Genericable<?, ?>) target).addDefinedToBe(defined);
//            return returns;
            return this.project((Genericable<?, ?>) target,null);
        }
        if(target instanceof Function<?,?>){
            return ((Function<?, ?>) target).returns().supposedToBe();
        }

        return target.ast().Error;
    }
    private Outline project(Outline target, AbstractNode argument) {
//        ProjectSession session = new ProjectSession();
        //hlf function call
        if (target instanceof Genericable) {
            return project((Genericable<?, ?>) target, argument);
        }
        //normal function call
        if (target instanceof FirstOrderFunction) {
            return project((FirstOrderFunction) target, argument);

        }
        GCPErrorReporter.report(argument, GCPErrCode.NOT_A_FUNCTION);
        return argument.ast().Error;
    }

    /**
     * HOF函数调用
     * HOF函数调用前没有函数定义，所以建立一个虚拟的函数定义
     * 并将该函数定义放进Generic的defined_to_be
     *
     * @param generic  HLF的函数调用
     * @param argument HFL函数调用的参数
     * @return 虚拟的函数返回
     */
    private Outline project(Genericable<?, ?> generic, AbstractNode argument) {
        if (generic.definedToBe() instanceof HigherOrderFunction) {
            return ((HigherOrderFunction) generic.definedToBe()).returns();
        }
        if(generic.definedToBe() instanceof Poly){
            Optional<Outline> option = ((Poly) generic.definedToBe()).options().stream().filter(o -> o instanceof HigherOrderFunction).findFirst();
            if(option.isPresent()){
                return ((HigherOrderFunction)option.get()).returns();
            }
        }
        Returnable returns = Return.from(generic.node());
        Outline argOutline = argument==null?generic.node().ast().Unit:argument.outline();
        HigherOrderFunction defined = new HigherOrderFunction(generic.node(), argOutline, returns);
        generic.addDefinedToBe(defined);
        return returns;
    }

    /**
     * FOF函数调用
     * 环境上下文中招到函数定义
     * 先投影参数，再投影返回值
     *
     * @param function 实际调用的函数定义
     * @param argument 要投影的参数节点
     */
    private Outline project(FirstOrderFunction function, AbstractNode argument) {
        ProjectSession session = function.getSession();
        if (session == null) {
            //开始一个projection session
            session = new ProjectSession();
        }
        session.copiedCache().clear();
//        Outline back = function;
        function = function.copy(session.copiedCache());
        //先投影参数
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
        //再投影返回值
        Outline result = function.returns().project(function.argument(), projectedArg, session);
        if (result instanceof FirstOrderFunction) {
            ((FirstOrderFunction) result).setSession(session);
        }
        //if this is the final projected, remove generics
        if(result instanceof Option){
            ((Option) result).options().removeIf(o->o instanceof Generic);
            if(((Option) result).options().size()==1){
                result = ((Option) result).options().getFirst();
            }
        }
        return result;

    }
}
