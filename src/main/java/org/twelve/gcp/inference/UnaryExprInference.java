package org.twelve.gcp.inference;

import org.twelve.gcp.exception.ErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.node.expression.UnaryExpression;
import org.twelve.gcp.node.expression.UnaryPosition;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.primitive.BOOL;
import org.twelve.gcp.outline.primitive.LONG;
import org.twelve.gcp.outline.primitive.NUMBER;

import static org.twelve.gcp.outline.Outline.Error;

public class UnaryExprInference implements Inference<UnaryExpression> {
    @Override
    public Outline infer(UnaryExpression node, Inferences inferences) {
        // Infer the type of the operand
        Outline ol = node.operand().infer(inferences);

        // Check the operator type and operand type compatibility
        switch (node.operator()) {
            case NEGATE: // -a
                // Ensure operand is numeric and operator is prefix
                if (ol instanceof NUMBER && node.position() == UnaryPosition.PREFIX) {
                    return ol;  // Return type remains the same for negation
                }
                break;

            case BANG: // !a
                // Ensure operand is boolean and operator is prefix
                if (ol instanceof BOOL && node.position() == UnaryPosition.PREFIX) {
                    return ol;  // Return type remains Boolean for logical NOT
                }
                break;

            case INCREMENT: // ++a or a++
            case DECREMENT: // --a or a--
                // Ensure operand is a long integer; handle both prefix and postfix positions
                if (ol instanceof LONG) {
                    return ol;  // Return type remains LONG for increment/decrement
                }
                break;

            default:
                // Throw an exception for unsupported operators
                ErrorReporter.report(node.operatorNode(), GCPErrCode.UNSUPPORTED_UNARY_OPERATION);
        }

        // Throw an exception for outline type mismatches or invalid positions
        ErrorReporter.report(node.operatorNode(), GCPErrCode.OUTLINE_MISMATCH);
        return Error;
    }

}
