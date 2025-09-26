package org.twelve.gcp.inference;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNKNOWN;

/**
 * calculate outline of value if there is
 * calculate outline of identifier
 * if identifier outline is unknown, set the identifier outline to value outline
 */
public class AssignmentInference implements Inference<Assignment> {
    @Override
    public Outline infer(Assignment node, Inferences inferences) {
        AST ast = node.ast();
        Outline valueOutline = node.rhs() == null?ast.Unknown:node.rhs().infer(inferences);
        Outline varOutline = node.lhs().infer(inferences);
        if (node.rhs() == null) {
            GCPErrorReporter.report(node.lhs(), GCPErrCode.NOT_INITIALIZED);
            return varOutline;
        }
        if(valueOutline.containsUnknown()){
            valueOutline = node.rhs().infer(inferences);
        }
//        Outline valueOutline = node.rhs().infer(inferences);


        if (valueOutline == ast.Ignore || valueOutline == ast.Unit) {
            GCPErrorReporter.report(node.rhs(), GCPErrCode.UNAVAILABLE_OUTLINE_ASSIGNMENT);
            return ast.Ignore;
        }
        if (valueOutline instanceof UNKNOWN) {
            GCPErrorReporter.report(node.rhs(), GCPErrCode.NOT_INITIALIZED);
            return ast.Ignore;
        }
        if (varOutline == ast.Nothing) {
            GCPErrorReporter.report(node.lhs(), GCPErrCode.VARIABLE_NOT_DEFINED);
            return ast.Ignore;
        }
        node.lhs().assign(node.ast().symbolEnv(), valueOutline);
        return ast.Ignore;
    }
}
