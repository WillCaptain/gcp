package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.node.expression.PolyNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Poly;

public class PolyInference implements Inference<PolyNode> {
    @Override
    public Outline infer(PolyNode node, Inferencer inferencer) {
        if (node.isOption()) {
            GCPErrorReporter.report(node, GCPErrCode.INVALID_OPTION_EXPRESSION,
                    "use '|' only in outline declarations: outline T = A|B");
            return node.ast().Error;
        }
        Outline[] outlines = node.nodes().stream().map(n -> n.infer(inferencer)).toArray(Outline[]::new);
        for (int i = 0; i < outlines.length; i++) {
            for (int j = 0; j < outlines.length; j++) {
                if (i != j && outlines[i].is(outlines[j])) {
                    GCPErrorReporter.report(node, GCPErrCode.POLY_SUM_FAIL,
                            outlines[i] + " and " + outlines[j] + " have an 'is' relationship — poly requires mutually unrelated types");
                    return node.ast().Error;
                }
            }
        }
        return Poly.from(node, outlines);
    }
}
