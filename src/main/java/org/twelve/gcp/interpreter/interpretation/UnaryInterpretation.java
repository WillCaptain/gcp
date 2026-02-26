package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.UnaryExpression;

public class UnaryInterpretation implements Interpretation<UnaryExpression> {
    @Override
    public Value interpret(UnaryExpression node, Interpreter interpreter) {
        Value operand = interpreter.eval(node.operand());
        return switch (node.operator().symbol()) {
            case "-" -> {
                if (operand instanceof IntValue iv)   yield new IntValue(-iv.value());
                if (operand instanceof FloatValue fv) yield new FloatValue(-fv.value());
                throw new RuntimeException("Cannot negate: " + operand);
            }
            case "!" -> BoolValue.of(!operand.isTruthy());
            case "~" -> {
                if (operand instanceof IntValue iv) yield new IntValue(~iv.value());
                throw new RuntimeException("Cannot bitwise-not: " + operand);
            }
            case "++" -> {
                if (operand instanceof IntValue iv) yield new IntValue(iv.value() + 1);
                throw new RuntimeException("Cannot increment: " + operand);
            }
            case "--" -> {
                if (operand instanceof IntValue iv) yield new IntValue(iv.value() - 1);
                throw new RuntimeException("Cannot decrement: " + operand);
            }
            default -> throw new RuntimeException("Unknown unary op: " + node.operator().symbol());
        };
    }
}
