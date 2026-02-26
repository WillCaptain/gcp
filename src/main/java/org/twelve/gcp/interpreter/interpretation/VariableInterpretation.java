package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.Variable;

/**
 * Variable nodes appear as LHS in assignments; when evaluated standalone they
 * resolve like identifiers.
 */
public class VariableInterpretation implements Interpretation<Variable> {
    @Override
    public Value interpret(Variable node, Interpreter interpreter) {
        Value v = interpreter.env().lookup(node.name());
        if (v != null) return v;
        throw new RuntimeException("Undefined variable: " + node.name());
    }
}
