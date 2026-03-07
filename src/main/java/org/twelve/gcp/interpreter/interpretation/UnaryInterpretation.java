package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.UnaryExpression;
import org.twelve.gcp.node.expression.UnaryPosition;
import org.twelve.gcp.node.expression.accessor.MemberAccessor;
import org.twelve.gcp.node.expression.identifier.Identifier;

public class UnaryInterpretation implements Interpretation<UnaryExpression> {
    @Override
    public Value interpret(UnaryExpression node, Interpreter interpreter) {
        Value operand = interpreter.eval(node.operand());
        return switch (node.operator().symbol()) {
            case "-" -> {
                if (operand instanceof IntValue iv)   yield IntValue.of(-iv.value());
                if (operand instanceof FloatValue fv) yield new FloatValue(-fv.value());
                throw new RuntimeException("Cannot negate: " + operand);
            }
            case "!" -> BoolValue.of(!operand.isTruthy());
            case "~" -> {
                if (operand instanceof IntValue iv) yield IntValue.of(~iv.value());
                throw new RuntimeException("Cannot bitwise-not: " + operand);
            }
            case "++" -> {
                if (!(operand instanceof IntValue iv))
                    throw new RuntimeException("Cannot increment: " + operand);
                IntValue newVal = IntValue.of(iv.value() + 1);
                writeBack(node.operand(), newVal, interpreter);
                yield node.position() == UnaryPosition.PREFIX ? newVal : iv;
            }
            case "--" -> {
                if (!(operand instanceof IntValue iv))
                    throw new RuntimeException("Cannot decrement: " + operand);
                IntValue newVal = IntValue.of(iv.value() - 1);
                writeBack(node.operand(), newVal, interpreter);
                yield node.position() == UnaryPosition.PREFIX ? newVal : iv;
            }
            default -> throw new RuntimeException("Unknown unary op: " + node.operator().symbol());
        };
    }

    /**
     * Writes the new value back to the operand's storage location.
     * Supports simple identifiers and entity-field accessors.
     */
    private static void writeBack(Expression operand, Value newVal, Interpreter interpreter) {
        if (operand instanceof Identifier id) {
            interpreter.env().set(id.name(), newVal);
        } else if (operand instanceof MemberAccessor ma) {
            Value target = interpreter.eval(ma.host());
            if (target instanceof EntityValue ev) {
                ev.setField(ma.member().name(), newVal);
            }
        }
    }
}
