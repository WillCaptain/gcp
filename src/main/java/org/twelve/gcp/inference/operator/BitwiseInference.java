package org.twelve.gcp.inference.operator;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;

public class BitwiseInference implements OperatorInference {
    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        // Bitwise operators operate on integer types
        if (left instanceof INTEGER && right instanceof INTEGER) {
            return new INTEGER(node);
        }
        if (left instanceof INTEGER && right instanceof Genericable<?,?>) {
            INTEGER r = new INTEGER(right.node());
            ((Genericable<?,?>) right).addDefinedToBe(r);
            return r;
        }
        if (right instanceof INTEGER && left instanceof Genericable<?,?>) {
            INTEGER l = new INTEGER(left.node());
            ((Genericable<?,?>) left).addDefinedToBe(l);
            return l;
        }
        if (left instanceof Generic && right instanceof Genericable<?,?>) {
            INTEGER l = new INTEGER(left.node());
            INTEGER r = new INTEGER(right.node());
            ((Genericable<?,?>) left).addDefinedToBe(l);
            ((Genericable<?,?>) right).addDefinedToBe(r);
            return new INTEGER(node);
        }
        ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
        return node.ast().Error;
    }
}
