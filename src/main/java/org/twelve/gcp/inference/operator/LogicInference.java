package org.twelve.gcp.inference.operator;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.projectable.Genericable;

public class LogicInference implements OperatorInference {
    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        AST ast = node.ast();
        // Logical operators typically operate on Boolean values
        if (left == ast.Boolean && right == ast.Boolean) {
            return ast.Boolean;
        }
        if (left == ast.Boolean && right instanceof Genericable<?,?>) {
            ((Genericable<?,?>) right).addDefinedToBe(ast.Boolean);
            return ast.Boolean;
        }
        if (right == ast.Boolean && left instanceof Genericable<?,?>) {
            ((Genericable<?,?>) left).addDefinedToBe(ast.Boolean);
            return ast.Boolean;
        }
        if (left instanceof Genericable<?,?> && right instanceof Genericable<?,?>) {
            ((Genericable<?,?>) left).addDefinedToBe(ast.Boolean);
            ((Genericable<?,?>) right).addDefinedToBe(ast.Boolean);
            return ast.Boolean;
        }
        GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH);
        return ast.Error;
    }
}
