package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.ThisNode;

public class ThisInterpretation implements Interpretation<ThisNode> {
    @Override
    public Value interpret(ThisNode node, Interpreter interpreter) {
        Value v = interpreter.env().lookup("this");
        return v != null ? v : UnitValue.INSTANCE;
    }
}
