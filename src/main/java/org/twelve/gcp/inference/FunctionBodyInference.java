package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.SumADT;
import org.twelve.gcp.outline.builtin.IGNORE;
import org.twelve.gcp.outline.projectable.Return;
import org.twelve.gcp.outline.projectable.Returnable;

public class FunctionBodyInference extends BodyInference<FunctionBody> {
    @Override
    public Outline infer(FunctionBody node, Inferences inferences) {
        Outline inferred = super.infer(node, inferences);
        if (inferred instanceof IGNORE) inferred = node.ast().Unit;
        if (inferred.containsIgnore()) {

            ((SumADT) inferred).options().removeIf(o -> o instanceof IGNORE);
            if(((SumADT) inferred).options().size()==1){
                inferred = ((SumADT) inferred).options().getFirst();
            }
            //((Option) inferred).options().add(node.ast().Unit);
            GCPErrorReporter.report(node, GCPErrCode.AMBIGUOUS_RETURN, "return type is expected");

        }
        Returnable returns = Return.from(node.parent());
        returns.addReturn(inferred);
//        returns.replaceIgnores();
        return returns;
    }
}
