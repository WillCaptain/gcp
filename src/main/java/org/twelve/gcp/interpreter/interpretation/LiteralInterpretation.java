package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.*;
import org.twelve.gcp.node.expression.LiteralNode;

import java.math.BigDecimal;

public class LiteralInterpretation implements Interpretation<LiteralNode<?>> {
    @Override
    public Value interpret(LiteralNode<?> node, Interpreter interpreter) {
        Object v = node.value();
        if (v instanceof Integer)    return new IntValue((long)(Integer) v);
        if (v instanceof Long)       return new IntValue((Long) v);
        if (v instanceof Double)     return new FloatValue((Double) v);
        if (v instanceof Float)      return new FloatValue((double)(Float) v);
        if (v instanceof BigDecimal) return new FloatValue(((BigDecimal) v).doubleValue());
        if (v instanceof String)     return new StringValue((String) v);
        if (v instanceof Boolean)    return BoolValue.of((Boolean) v);
        return UnitValue.INSTANCE;
    }
}
