package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.BinaryExpression;

import static org.twelve.gcp.interpreter.interpretation.BuiltinMethods.toDouble;

public class BinaryInterpretation implements Interpretation<BinaryExpression> {
    @Override
    public Value interpret(BinaryExpression node, Interpreter interpreter) {
        Value left = interpreter.eval(node.left());
        switch (node.operator()) {
            case LOGICAL_AND: return left.isTruthy() ? interpreter.eval(node.right()) : BoolValue.FALSE;
            case LOGICAL_OR:  return left.isTruthy() ? left : interpreter.eval(node.right());
            default: break;
        }
        Value right = interpreter.eval(node.right());
        return applyBinaryOp(node.operator().symbol(), left, right);
    }

    private Value applyBinaryOp(String op, Value left, Value right) {
        if (left instanceof IntValue && right instanceof IntValue) {
            long l = ((IntValue) left).value(), r = ((IntValue) right).value();
            return switch (op) {
                case "+"  -> new IntValue(l + r);
                case "-"  -> new IntValue(l - r);
                case "*"  -> new IntValue(l * r);
                case "/"  -> new IntValue(l / r);
                case "%"  -> new IntValue(l % r);
                case "==" -> BoolValue.of(l == r);
                case "!=" -> BoolValue.of(l != r);
                case ">"  -> BoolValue.of(l > r);
                case "<"  -> BoolValue.of(l < r);
                case ">=" -> BoolValue.of(l >= r);
                case "<=" -> BoolValue.of(l <= r);
                case "&"  -> new IntValue(l & r);
                case "|"  -> new IntValue(l | r);
                case "^"  -> new IntValue(l ^ r);
                case "<<" -> new IntValue(l << r);
                case ">>" -> new IntValue(l >> r);
                default   -> throw new RuntimeException("Unknown int op: " + op);
            };
        }
        if (left instanceof FloatValue || right instanceof FloatValue) {
            double l = toDouble(left), r = toDouble(right);
            return switch (op) {
                case "+"  -> new FloatValue(l + r);
                case "-"  -> new FloatValue(l - r);
                case "*"  -> new FloatValue(l * r);
                case "/"  -> new FloatValue(l / r);
                case "%"  -> new FloatValue(l % r);
                case "==" -> BoolValue.of(l == r);
                case "!=" -> BoolValue.of(l != r);
                case ">"  -> BoolValue.of(l > r);
                case "<"  -> BoolValue.of(l < r);
                case ">=" -> BoolValue.of(l >= r);
                case "<=" -> BoolValue.of(l <= r);
                default   -> throw new RuntimeException("Unknown float op: " + op);
            };
        }
        if ("+".equals(op)) return new StringValue(left.display() + right.display());
        if ("==".equals(op)) return BoolValue.of(left.equals(right));
        if ("!=".equals(op)) return BoolValue.of(!left.equals(right));
        throw new RuntimeException("Cannot apply op '" + op + "' to " + left + " and " + right);
    }
}
