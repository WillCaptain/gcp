package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.As;
import org.twelve.gcp.outline.Outline;

public class AsInference implements Inference<As> {
    @Override
    public Outline infer(As node, Inferences inferences) {
        Outline var = node.expression().infer(inferences);
        Outline as = node.as().infer(inferences);
        if (!var.is(as)) {
            ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, var + " is not " + as);
        }
        return as;
    }
}
