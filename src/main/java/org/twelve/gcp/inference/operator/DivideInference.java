package org.twelve.gcp.inference.operator;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.*;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.NumericAble;
import org.twelve.gcp.outline.projectable.OperateAble;

import static org.twelve.gcp.common.Tool.getExactNumberOutline;

/**
 * Type inference for Python's true-division operator {@code /}.
 *
 * <p>Python semantics: {@code int / int → float} (always returns float, unlike {@code //}).
 * When at least one operand is already float/double/decimal, the result is float.
 * For generic/unknown operands, falls back to {@link NumericAble} deferral.
 */
public class DivideInference implements OperatorInference {

    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        AST ast = node.ast();
        // Both operands are concrete numeric types
        if (left instanceof NUMBER && right instanceof NUMBER) {
            // Python's / always returns float even for int / int
            if (left instanceof INTEGER || left instanceof LONG) {
                if (right instanceof INTEGER || right instanceof LONG) {
                    return ast.Float;  // int / int → float
                }
            }
            return getExactNumberOutline(left, right);
        }
        // Deferred numeric result — keep deferring
        if (left instanceof NumericAble || right instanceof NumericAble) {
            return new NumericAble(left, right, node);
        }
        if (left instanceof NUMBER && right instanceof OperateAble) {
            ((OperateAble<?>) right).addDefinedToBe(ast.Number);
            return right;
        }
        if (right instanceof NUMBER && left instanceof OperateAble) {
            ((OperateAble<?>) left).addDefinedToBe(ast.Number);
            return left;
        }
        if (left instanceof Generic && right instanceof OperateAble) {
            ((OperateAble<?>) left).addDefinedToBe(ast.Number);
            ((OperateAble<?>) right).addDefinedToBe(ast.Number);
            return new NumericAble(left, right, node);
        }
        GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, node + " is invalid");
        return ast.Number;
    }
}
