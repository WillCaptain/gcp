package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.StringValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.identifier.Identifier;

public class IdentifierInterpretation implements Interpretation<Identifier> {
    @Override
    public Value interpret(Identifier node, Interpreter interpreter) {
        String name = node.name();
        if (name.startsWith("__") && name.endsWith("__")) {
            return new StringValue(stripUnderscores(name));
        }
        Value v = interpreter.env().lookup(name);
        if (v != null) return v;
        throw new RuntimeException("Undefined variable: " + name);
    }

    private static String stripUnderscores(String name) {
        if (name.length() > 4) return name.substring(2, name.length() - 2);
        return name;
    }
}
