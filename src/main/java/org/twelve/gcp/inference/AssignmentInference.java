package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Identifier;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.node.statement.VariableDeclarator;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.builtin.UNKNOWN;
import org.twelve.gcp.outlineenv.EnvSymbol;

import static org.twelve.gcp.common.Tool.cast;
import static org.twelve.gcp.outline.Outline.*;

/**
 * calculate outline of value if there is
 * calculate outline of identifier
 * if identifier outline is unknown, set the identifier outline to value outline
 */
public class AssignmentInference implements Inference<Assignment> {
    @Override
    public Outline infer(Assignment node, Inferences inferences) {
        Outline varOutline = node.lhs().infer(inferences);
        if (node.rhs() == null) {
            ErrorReporter.report(node.lhs(), GCPErrCode.NOT_INITIALIZED);
            return varOutline;
        }
        Outline valueOutline = node.rhs() == null ? Unknown : node.rhs().infer(inferences);


        if (valueOutline == Ignore || valueOutline == Unit) {
            ErrorReporter.report(node.rhs(), GCPErrCode.UNAVAILABLE_OUTLINE_ASSIGNMENT);
            return Ignore;
        }
        if (valueOutline instanceof UNKNOWN) {
            ErrorReporter.report(node.rhs(), GCPErrCode.NOT_INITIALIZED);
            return Ignore;
        }
        if (varOutline == Nothing) {
            ErrorReporter.report(node.lhs(), GCPErrCode.VARIABLE_NOT_DEFINED);
            return Ignore;
        }
        node.lhs().assign(node.ast().symbolEnv(), valueOutline);
        return Ignore;
    }
}
