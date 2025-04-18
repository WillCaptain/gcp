package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.BOOL;

public class ArmInference implements Inference<Arm>{
    @Override
    public Outline infer(Arm node, Inferences inferences) {
        if(!(node.test().infer(inferences) instanceof BOOL)){
            ErrorReporter.report(node.test(), GCPErrCode.CONDITION_IS_NOT_BOOL);
        }
        return node.consequence().infer(inferences);
    }
}
