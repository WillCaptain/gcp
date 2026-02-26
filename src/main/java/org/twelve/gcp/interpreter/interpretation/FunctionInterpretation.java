package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.FunctionValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.function.FunctionNode;

public class FunctionInterpretation implements Interpretation<FunctionNode> {
    @Override
    public Value interpret(FunctionNode node, Interpreter interpreter) {
        return new FunctionValue(node, interpreter.env());
    }
}
