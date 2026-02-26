package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;

public class BinaryExprInference implements Inference<BinaryExpression> {
    @Override
    public Outline infer(BinaryExpression node, Inferencer inferencer) {
        Outline left = node.left().infer(inferencer);
        Outline right = node.right().infer(inferencer);
        Outline inferred = node.operator().infer(left, right, node);
        if (inferred == node.ast().Error) {
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
            return node.ast().Error;
        } else {
            return inferred;
        }
    }


}
