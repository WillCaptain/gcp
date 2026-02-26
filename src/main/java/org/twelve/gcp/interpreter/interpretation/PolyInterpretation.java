package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.PolyNode;

public class PolyInterpretation implements Interpretation<PolyNode> {
    @Override
    public Value interpret(PolyNode node, Interpreter interpreter) {
        for (Node child : node.nodes()) {
            return interpreter.eval(child);
        }
        return UnitValue.INSTANCE;
    }
}
