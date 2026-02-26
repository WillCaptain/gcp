package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.As;

public class AsInterpretation implements Interpretation<As> {
    @Override
    public Value interpret(As node, Interpreter interpreter) {
        return interpreter.eval(node.expression());
    }
}
