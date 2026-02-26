package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.BoolValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.IsAs;

public class IsAsInterpretation implements Interpretation<IsAs> {
    @Override
    public Value interpret(IsAs node, Interpreter interpreter) {
        Value subject = interpreter.eval(node.a());
        if (node.c() != null) {
            interpreter.env().define(node.c().name(), subject);
        }
        return BoolValue.TRUE;
    }
}
