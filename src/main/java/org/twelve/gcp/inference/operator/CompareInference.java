package org.twelve.gcp.inference.operator;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;

public class CompareInference implements OperatorInference {
    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        if(left instanceof Genericable<?,?>){
            ((Genericable<?,?>) left).addDefinedToBe(right);
        }
        if (left.is(right) || right.is(left)) {
            return node.ast().Boolean;
        }
        ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
        return node.ast().Error;
    }
}
