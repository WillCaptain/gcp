package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.ReturnException;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.statement.ReturnStatement;

public class ReturnInterpretation implements Interpretation<ReturnStatement> {
    @Override
    public Value interpret(ReturnStatement node, Interpreter interpreter) {
        Value v = node.expression() != null ? interpreter.eval(node.expression()) : UnitValue.INSTANCE;
        throw new ReturnException(v);
    }
}
