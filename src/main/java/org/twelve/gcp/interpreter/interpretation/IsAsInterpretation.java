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
        var binding = node.c();
        if (binding != null) {
            interpreter.env().define(binding.name(), subject);
        }
        return BoolValue.TRUE;
    }
}
