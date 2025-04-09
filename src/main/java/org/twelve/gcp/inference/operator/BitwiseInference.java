package org.twelve.gcp.inference.operator;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.INTEGER;
import org.twelve.gcp.outline.projectable.Generic;

public class BitwiseInference implements OperatorInference {
    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        // Bitwise operators operate on integer types
        if (left instanceof INTEGER && right instanceof INTEGER) {
            return Outline.Integer;
        }
        if (left instanceof INTEGER && right instanceof Generic) {
            ((Generic) right).addDefinedToBe(Outline.Integer);
            return Outline.Integer;
        }
        if (right instanceof INTEGER && left instanceof Generic) {
            ((Generic) left).addDefinedToBe(Outline.Integer);
            return Outline.Integer;
        }
        if (left instanceof Generic && right instanceof Generic) {
            ((Generic) left).addDefinedToBe(Outline.Integer);
            ((Generic) right).addDefinedToBe(Outline.Integer);
            return Outline.Integer;
        }
        ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
        return Outline.Error;
    }
}
