package org.twelve.gcp.inference.operator;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.Genericable;

public class LogicInference implements OperatorInference {
    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        // Logical operators typically operate on Boolean values
        if (left == Outline.Boolean && right == Outline.Boolean) {
            return Outline.Boolean;
        }
        if (left == Outline.Boolean && right instanceof Genericable<?,?>) {
            ((Genericable<?,?>) right).addDefinedToBe(Outline.Boolean);
            return Outline.Boolean;
        }
        if (right == Outline.Boolean && left instanceof Genericable<?,?>) {
            ((Genericable<?,?>) left).addDefinedToBe(Outline.Boolean);
            return Outline.Boolean;
        }
        if (left instanceof Genericable<?,?> && right instanceof Genericable<?,?>) {
            ((Genericable<?,?>) left).addDefinedToBe(Outline.Boolean);
            ((Genericable<?,?>) right).addDefinedToBe(Outline.Boolean);
            return Outline.Boolean;
        }
        ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
        return Outline.Error;
    }
}
