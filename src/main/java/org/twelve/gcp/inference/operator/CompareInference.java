package org.twelve.gcp.inference.operator;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.BinaryExpression;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.MatchTest;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.primitive.ANY;
import org.twelve.gcp.outline.primitive.NOTHING;
import org.twelve.gcp.outline.projectable.Genericable;

public class CompareInference implements OperatorInference {
    @Override
    public Outline infer(Outline left, Outline right, BinaryExpression node) {
        if (right instanceof NOTHING && left instanceof Genericable<?,?> genericLeft) {
            return compareGenericWithNothing(genericLeft, node);
        }
        if (left instanceof NOTHING && right instanceof Genericable<?,?> genericRight) {
            return compareGenericWithNothing(genericRight, node);
        }
        if(left instanceof Genericable<?,?>){
            ((Genericable<?,?>) left).addDefinedToBe(right);
        }
        if (left.is(right) || right.is(left)) {
            return node.ast().Boolean;
        }
        return reportMismatch(node, left, right);
    }

    private Outline compareGenericWithNothing(Genericable<?, ?> generic, BinaryExpression node) {
        Outline declared = generic.declaredToBe();
        if (declared instanceof ANY) {
            return node.ast().Boolean;
        }
        if (declared instanceof Option opt
                && opt.options().stream().anyMatch(o -> o instanceof NOTHING)) {
            return node.ast().Boolean;
        }
        return reportMismatch(node, generic, node.ast().Nothing);
    }

    private Outline reportMismatch(BinaryExpression node, Outline left, Outline right) {
        String message = "Comparison between `" + left + "` and `" + right + "` is always false in this context.";
        if (isInConditionContext(node)) {
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH_IN_CONDITION, message);
            // Condition mismatch is suspicious but still a valid boolean comparison;
            // returning Bool avoids cascading "condition is not bool" hard errors.
            return node.ast().Boolean;
        } else {
            GCPErrorReporter.report(node, GCPErrCode.OUTLINE_MISMATCH, message);
            return node.ast().Error;
        }
    }

    private boolean isInConditionContext(Expression expr) {
        var current = expr;
        while (current.parent() != null) {
            var parent = current.parent();
            if (parent instanceof Arm arm) {
                return arm.test() == current || isDescendantOf(current, arm.test());
            }
            if (parent instanceof MatchTest matchTest) {
                return matchTest.condition() != null
                        && (matchTest.condition() == current || isDescendantOf(current, matchTest.condition()));
            }
            if (!(parent instanceof Expression)) {
                return false;
            }
            current = (Expression) parent;
        }
        return false;
    }

    private boolean isDescendantOf(Expression node, Expression root) {
        var cursor = node;
        while (cursor != null) {
            if (cursor == root) return true;
            if (!(cursor.parent() instanceof Expression)) return false;
            cursor = (Expression) cursor.parent();
        }
        return false;
    }
}
