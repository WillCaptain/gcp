package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.PolyValue;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.PolyNode;

import java.util.ArrayList;
import java.util.List;

public class PolyInterpretation implements Interpretation<PolyNode> {
    @Override
    public Value interpret(PolyNode node, Interpreter interpreter) {
        List<Value> options = new ArrayList<>();
        for (Node child : node.nodes()) {
            options.add(interpreter.eval(child));
        }
        if (options.isEmpty()) return UnitValue.INSTANCE;
        // Single-option poly degrades to the bare value (no boxing overhead)
        if (options.size() == 1) return options.get(0);
        return new PolyValue(options);
    }
}
