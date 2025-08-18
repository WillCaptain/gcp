package org.twelve.gcp.inference;

import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;

/**
 * add identifier into symbol environment as unknown outline before the inference
 */
public class VariableDeclaratorInference implements Inference<VariableDeclarator> {
    @Override
    public Outline infer(VariableDeclarator node, Inferences inferences) {
        for (Assignment assignment : node.assignments()) {
            assignment.infer(inferences);
        }
//        LocalSymbolEnvironment oEnv = node.ast().symbolEnv();
//        Set<Long> cache = node.ast().cache();
//        for (Assignment assignment : node.assignments()) {
//            Identifier var = cast(assignment.lhs());
//            EnvSymbol symbol = oEnv.current().lookupSymbol(var.name());//find the symbol in current scope
//            if (symbol != null) {//there is symbol in current scope,can't have duplicate declaration
//                if (symbol.node() != var && !(node.parent() instanceof EntityNode)) {
//                    ErrorReporter.report(node, GCPErrCode.DUPLICATED_DEFINITION);
//                    return Ignore;
//                }
//            } else {
//                oEnv.defineSymbol(var.name(), var.outline(),
//                        node.kind().mutable(), var.isDeclared(), var);
//                assignment.infer(inferences);
//                cache.add(var.id());
//            }
//            assignment.infer(inferences);
//        }
        return node.ast().Ignore;
    }

//    private static void inferAssignment(Inferences inferences, Assignment assignment, EnvSymbol symbol) {
//        Outline valueOutline = assignment.rhs() == null ? Nothing : assignment.rhs().infer(inferences);
//        if (valueOutline == Ignore || valueOutline == Unit) {
//            ErrorReporter.report(assignment.rhs(), GCPErrCode.UNAVAILABLE_OUTLINE_ASSIGNMENT);
//            valueOutline = Error;
//        }
//        if (valueOutline instanceof UNKNOWN) {
//            ErrorReporter.report(assignment.rhs(), GCPErrCode.NOT_INITIALIZED);
//        }
//        symbol.update(valueOutline);
////        assignment.infer(inferences);
//    }
}
