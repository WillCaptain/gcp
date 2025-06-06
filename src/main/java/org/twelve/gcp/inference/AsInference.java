package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.As;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.OutlineWrapper;

import static org.twelve.gcp.common.Tool.cast;

public class AsInference implements Inference<As> {
    @Override
    public Outline infer(As node, Inferences inferences) {
        Outline var = node.expression().infer(inferences);
        OutlineWrapper as = cast(node.as().infer(inferences));
        if (!var.is(as.outline())) {
            ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, var + " is not " + as);
        }
        return as.outline();
    }
}
