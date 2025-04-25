package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.EntityNode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.EnvSymbol;
import org.twelve.gcp.outlineenv.LocalSymbolEnvironment;

import java.util.Objects;
import java.util.Set;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.*;

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
            EnvSymbol symbol = oEnv.current().lookup(var.token());//find the symbol in current scope
            if (symbol != null) {//there is symbol in current scope,can't have duplicate declaration
                if (symbol.originNode() != var && !(node.parent() instanceof EntityNode)) {
                    ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
                    return Ignore;
                }

//                if (assignment.rhs() == null) return Ignore;
//                Outline inferred = assignment.rhs().infer(inferences);
//                if (!inferred.equals(symbol.outline())){//类型不同，如果没有声明类型，开始重载定义，如果有声明类型，不能重复定义
//
//                    if ((symbol.isDeclared() || var.isDeclared())&&!cache.contains(var.id())) {
//                        ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
//                    }else {
//                        if(symbol.outline() instanceof UNKNOWN){
//                            assignment.infer(inferences);
//                        }else {
//                            Poly poly = Poly.create();
//                            //隐式动态poly重载
//                            poly.sum(symbol.outline(), symbol.mutable());
//                            if (!poly.sum(inferred, node.kind().mutable())) {
//                                ErrorReporter.report(node, GCPErrCode.POLY_SUM_FAIL);
//                                return Ignore;
//                            }
//                            symbol.polyTo(poly);
//                        }
//                        cache.add(var.id());
//                    }
//                    assignment.lhs().infer(inferences);
//                    assignment.setInferred();//not good solution
//                }
            } else {
                oEnv.defineSymbol(var.token(), var.outline(),
                        node.kind().mutable(), var.isDeclared(), var);
                assignment.infer(inferences);
                cache.add(var.id());
            }
            assignment.infer(inferences);
        }
        return Ignore;
    }

    private static void inferAssignment(Inferences inferences, Assignment assignment, EnvSymbol symbol) {
        Outline valueOutline = assignment.rhs() == null ? Nothing : assignment.rhs().infer(inferences);
        if (valueOutline == Ignore || valueOutline == Unit) {
            ErrorReporter.report(assignment.rhs(), GCPErrCode.UNAVAILABLE_OUTLINE_ASSIGNMENT);
            valueOutline = Error;
        }
        if (valueOutline instanceof UNKNOWN) {
            ErrorReporter.report(assignment.rhs(), GCPErrCode.NOT_INITIALIZED);
        }
        symbol.update(valueOutline);
//        assignment.infer(inferences);
    }
}
