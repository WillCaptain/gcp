package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;

public class BinaryExprInference implements Inference<BinaryExpression> {
    @Override
    public Outline infer(BinaryExpression node, Inferences inferences) {
        Outline left = node.left().infer(inferences);
        Outline right = node.right().infer(inferences);
        Outline inferred = node.operator().infer(left, right, node);
        if (inferred == Outline.Error) {
            ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
            return Outline.Error;
        } else {
            return inferred;
        }
    }


}
