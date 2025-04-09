package org.twelve.gcp.inference;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.projectable.*;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.List;
import java.util.stream.Collectors;

import static org.twelve.gcp.common.Tool.cast;

public class FunctionCallInference implements Inference<FunctionCallNode> {
    @Override
    public Outline infer(FunctionCallNode node, Inferences inferences) {
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
//        Outline supposed = oEnv.lookup(node.function().symbolName()).outline();
        Outline func = node.function().infer(inferences);
        if (func == null) {
            ErrorReporter.report(node, GCPErrCode.FUNCTION_NOT_DEFINED);
            return Outline.Error;
        }
        if(func==Outline.Unknown){
            return func;
        }
        Outline result;
        //如果有重载方法
        if (func instanceof Poly) {
            result = targetOverride(cast(func), node.arguments(), inferences, node);
//            if (result == Outline.Error) {
//                ErrorReporter.report(node, GCPErrCode.FUNCTION_NOT_DEFINED);
//                return Outline.Error;
//            }
        } else {
            result = func;
        }

        if(node.arguments().size()>0) {
            //按顺序投影参数
            for (Expression argument : node.arguments()) {
                argument.infer(inferences);
                result = project(result, argument);
//                if (result == Outline.Error) {
//                    ErrorReporter.report(argument, GCPErrCode.PROJECT_FAIL);
//                }
            }
        }else{
            result = ((Function)result).returns().supposedToBe();
        }
        return result;
    }

    private Outline targetOverride(Poly overwrite, List<Expression> arguments, Inferences inferences, FunctionCallNode node) {
        List<Function> fs = overwrite.options().stream().filter(o->o instanceof Function).map(o->(Function)o).collect(Collectors.toList());
        for (Function f : fs) {
            if (this.matchFunction(f, arguments, inferences, node)) {
                return f;
            }
        }
        return Outline.Error;
    }

    private boolean matchFunction(Function function, List<Expression> arguments, Inferences inferences, FunctionCallNode node) {
        Function f = null;
        for (Expression argument : arguments) {
            if (f == null) {//匹配第一个参数
                f = function;
            } else {//匹配第n参数
                if (function.returns().supposedToBe() instanceof Function) {
                    f = cast(function.returns().supposedToBe());
                } else {
                    ErrorReporter.report(node, GCPErrCode.ARGUMENT_MISMATCH);
                    return false;
                }
            }
            Outline arg = argument.infer(inferences);
            if (!arg.is(f.argument())) {
                ErrorReporter.report(node, GCPErrCode.ARGUMENT_MISMATCH);
                return false;
            }
        }
        return true;
    }

    private Outline project(Outline target, Node argument) {
//        ProjectSession session = new ProjectSession();
        //hlf function call
        if (target instanceof Genericable) {
            return project((Genericable) target, argument);
        }
        //normal function call
        if (target instanceof FirstOrderFunction) {
            return  project((FirstOrderFunction) target, argument);

        }
        ErrorReporter.report(argument, GCPErrCode.NOT_A_FUNCTION);
        return Outline.Error;
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
    private Outline project(Genericable generic, Node argument) {
        Return returns = Return.from(generic.node());
        HigherOrderFunction defined = new HigherOrderFunction(generic.node(), cast(argument.outline()), returns);
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
     * @return
     */
    private Outline project(FirstOrderFunction function, Node argument) {
        ProjectSession session = function.getSession();
        if(session==null){
            //开始一个projection session
            session = new ProjectSession();
        }

        //先投影参数
        Outline projectedArg = function.argument().project(function.argument(), argument.outline(), session);
//        if (function.argument().project(function.argument(), argument.outline(), session) == Outline.Error) {
//            return Outline.Error;
//        }
        //再投影返回值
        Outline result = function.returns().project(function.argument(), projectedArg, session);
        if(result instanceof FirstOrderFunction){
            ((FirstOrderFunction) result).setSession(session);
        }
        return result;

    }
}
