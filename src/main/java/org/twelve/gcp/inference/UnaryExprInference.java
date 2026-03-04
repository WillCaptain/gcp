package org.twelve.gcp.inference;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.UnaryExpression;
import org.twelve.gcp.node.expression.UnaryPosition;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.BOOL;
import org.twelve.gcp.outline.primitive.LONG;
import org.twelve.gcp.outline.primitive.NUMBER;
import org.twelve.gcp.outline.projectable.OperateAble;

public class UnaryExprInference implements Inference<UnaryExpression> {
    @Override
    public Outline infer(UnaryExpression node, Inferencer inferencer) {
        Outline ol = node.operand().infer(inferencer);

        switch (node.operator()) {
            case NEGATE: // -a
                if (ol instanceof NUMBER && node.position() == UnaryPosition.PREFIX) {
                    return ol;
                }
                // unknown/generic operand: constrain to Number and propagate
                if (ol instanceof OperateAble && node.position() == UnaryPosition.PREFIX) {
                    ((OperateAble<?>) ol).addDefinedToBe(node.ast().Number);
                    return ol;
                }
                break;

            case BANG: // !a
                if (ol instanceof BOOL && node.position() == UnaryPosition.PREFIX) {
                    return ol;
                }
                if (ol instanceof OperateAble && node.position() == UnaryPosition.PREFIX) {
                    ((OperateAble<?>) ol).addDefinedToBe(node.ast().Boolean);
                    return ol;
                }
                break;

            case INCREMENT: // ++a or a++
            case DECREMENT: // --a or a--
                if (ol instanceof LONG) {
                    return ol;
                }
                if (ol instanceof OperateAble) {
                    ((OperateAble<?>) ol).addDefinedToBe(node.ast().Long);
                    return ol;
                }
                break;

            default:
                break;
        }

        GCPErrorReporter.report(node.operand(), GCPErrCode.UNSUPPORTED_UNARY_OPERATION,
                ol.toString() + " is not supported in unary operation.");
        return ol;
    }

}
