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
            // `__xxx__` is the host-plugin bridge. A host may register a callable
            // (e.g. curried FunctionValue for multi-arg plugins such as `__llm__`)
            // under the stripped id; fall back to StringValue so that legacy
            // `__builder__<T>` reference-call dispatch via constructors continues
            // to work when no direct callable is registered.
            String stripped = stripUnderscores(name);
            Value bound = interpreter.env().lookup(stripped);
            if (bound != null) return bound;
            return new StringValue(stripped);
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
