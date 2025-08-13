package org.twelve.gcp.inference.operator;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.NUMBER;
import org.twelve.gcp.outline.projectable.Addable;
import org.twelve.gcp.outline.projectable.Generic;
import org.twelve.gcp.outline.projectable.NumericAble;
import org.twelve.gcp.outline.projectable.OperateAble;

import static org.twelve.gcp.common.Tool.getExactNumberOutline;

public class NumOperaInference implements OperatorInference {
    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        AST ast = node.ast();
        // left and right are both number
        if (left instanceof NUMBER && right instanceof NUMBER) {
            return getExactNumberOutline(left, right);
        }
        //left and right is number or generic
        if (left instanceof NUMBER && right instanceof OperateAble) {
            ((OperateAble<?>) right).addDefinedToBe(ast.Number);
            return right;
        }
        if (right instanceof NUMBER && left instanceof OperateAble) {
            ((OperateAble<?>) left).addDefinedToBe(ast.Number);
            return left;
        }
        //left and right are both generic
        if (left instanceof Generic && right instanceof OperateAble) {
            ((OperateAble<?>) left).addDefinedToBe(ast.Number);
            ((OperateAble<?>) right).addDefinedToBe(ast.Number);
            return new NumericAble(left, right, node);
        }
        ErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH,node+" is invalid");
        return ast.Number;
    }
}
