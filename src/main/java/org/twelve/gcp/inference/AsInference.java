package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.As;
import org.twelve.gcp.outline.Outline;

public class AsInference implements Inference<As> {
    @Override
    public Outline infer(As node, Inferencer inferencer) {
        Outline var = node.expression().infer(inferencer);
        Outline as = node.as().infer(inferencer);
        if (!var.is(as)) {
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, var + " is not " + as);
        }
        return as;
    }
}
