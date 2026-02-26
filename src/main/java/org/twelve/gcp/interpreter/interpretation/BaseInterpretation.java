package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.BaseNode;

public class BaseInterpretation implements Interpretation<BaseNode> {
    @Override
    public Value interpret(BaseNode node, Interpreter interpreter) {
        Value v = interpreter.env().lookup("base");
        return v != null ? v : UnitValue.INSTANCE;
    }
}
