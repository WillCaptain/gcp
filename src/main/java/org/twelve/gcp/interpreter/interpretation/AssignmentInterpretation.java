package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.Assignment;

public class AssignmentInterpretation implements Interpretation<Assignment> {
    @Override
    public Value interpret(Assignment node, Interpreter interpreter) {
        Value rhs = interpreter.eval(node.rhs());
        UnpackBinder.bindAssignable(node.lhs(), rhs, interpreter);
        return rhs;
    }
}
