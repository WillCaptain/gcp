package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.DictValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.DictNode;
import org.twelve.gcp.node.expression.Expression;

import java.util.LinkedHashMap;
import java.util.Map;

public class DictInterpretation implements Interpretation<DictNode> {
    @Override
    public Value interpret(DictNode node, Interpreter interp) {
        LinkedHashMap<Value, Value> entries = new LinkedHashMap<>();
        for (Map.Entry<Expression, Expression> entry : node.values().entrySet()) {
            entries.put(interp.eval(entry.getKey()), interp.eval(entry.getValue()));
        }
        return new DictValue(entries);
    }
}
