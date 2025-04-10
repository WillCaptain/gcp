package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.Set;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.Ignore;

/**
 * add identifier into symbol environment as unknown outline before the inference
 */
public class VariableDeclaratorInference implements Inference<VariableDeclarator> {
    @Override
    public Outline infer(VariableDeclarator node, Inferences inferences) {
        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
        Set<Long> cache = node.ast().cache();
        for (Assignment assignment : node.assignments()) {
            Identifier var = cast(assignment.lhs());
            EnvSymbol symbol = oEnv.lookup(var.token());
            //符号表里是否已经有了符号
            if (symbol != null && symbol.scope() == node.scope()) {
                if (assignment.rhs() == null) return Ignore;
                Outline inferred = assignment.rhs().infer(inferences);
                if (inferred.equals(symbol.outline())) {
                    if(!cache.contains(var.id())){//相同的类型，不能重复定义
                        ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
                    }
                } else {//类型不同，如果没有声明类型，开始重载定义，如果有声明类型，不能重复定义
                    if ((symbol.isDeclared() || var.isDeclared())&&!cache.contains(var.id())) {
                        ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
                    }else {
                        Poly poly = Poly.create();
                        //隐式动态poly重载
                        poly.sum(symbol.outline(), symbol.originNode(), symbol.isMutable());
                        if (!poly.sum(inferred, var, node.kind().mutable())) {
                            ErrorReporter.report(node, GCPErrCode.POLY_SUM_FAIL);
                            return Ignore;
                        }
                        symbol.polyTo(poly);
                        cache.add(var.id());
                    }
                    assignment.lhs().infer(inferences);
                    assignment.setInferred();//not good solution
                }
            } else {
                symbol = oEnv.defineSymbol(var.token(), var.outline(),
                        node.kind().mutable(), var.isDeclared(), var);
                inferAssignment(inferences, assignment, symbol);
                cache.add(var.id());
            }
        }
        return Ignore;
    }

    private static void inferAssignment(Inferences inferences, Assignment assignment, EnvSymbol symbol) {
        if (assignment.rhs() == null) {
            if (!symbol.isMutable()) {
                ErrorReporter.report(assignment.lhs(), GCPErrCode.NOT_INITIALIZED);
            }
        } else {
            assignment.infer(inferences);
        }
    }
}
