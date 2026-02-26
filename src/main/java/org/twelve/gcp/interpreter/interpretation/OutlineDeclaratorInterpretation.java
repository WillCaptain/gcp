package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.statement.OutlineDeclarator;

/** Type declarations are compile-time only; at runtime they produce no value. */
public class OutlineDeclaratorInterpretation implements Interpretation<OutlineDeclarator> {
    @Override
    public Value interpret(OutlineDeclarator node, Interpreter interpreter) {
        return UnitValue.INSTANCE;
    }
}
