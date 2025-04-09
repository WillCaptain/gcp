package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.statement.Assignment;
import org.twelve.gcp.outline.Outline;

import static org.twelve.gcp.outline.Outline.*;

/**
 * calculate outline of value if there is
 * calculate outline of identifier
 * if identifier outline is unknown, set the identifier outline to value outline
 */
public class AssignmentInference implements Inference<Assignment> {
    @Override
    public Outline infer(Assignment node, Inferences inferences) {
        Outline valueOutline = node.rhs() == null ? Nothing : node.rhs().infer(inferences);


        if (valueOutline == Ignore || valueOutline == Unit) {
            ErrorReporter.report(GCPErrCode.UNAVAILABLE_OUTLINE_ASSIGNMENT);
            return Ignore;
        }
        Outline varOutline = node.lhs().infer(inferences);
        if (varOutline == Nothing) {
            ErrorReporter.report(node.lhs(), GCPErrCode.VARIABLE_NOT_DEFINED);
            return Ignore;
        }

//        try {
            node.lhs().assign(node.ast().symbolEnv(), valueOutline);
//        }catch(GCPRuntimeException e){
//            ErrorReporter.report(node.rhs(),e.errCode());
//        }
        return Ignore;
    }
}
