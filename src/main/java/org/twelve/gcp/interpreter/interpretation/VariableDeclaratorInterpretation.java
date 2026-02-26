package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.statement.VariableDeclarator;

public class VariableDeclaratorInterpretation implements Interpretation<VariableDeclarator> {
    @Override
    public Value interpret(VariableDeclarator node, Interpreter interpreter) {
        Value last = UnitValue.INSTANCE;
        for (var assignment : node.assignments()) {
            last = interpreter.eval(assignment);
        }
        return last;
    }
}
