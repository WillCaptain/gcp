package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.statement.ExpressionStatement;

public class ExpressionStatementInterpretation implements Interpretation<ExpressionStatement> {
    @Override
    public Value interpret(ExpressionStatement node, Interpreter interp) {
        Value last = UnitValue.INSTANCE;
        for (Expression expr : node.expressions()) {
            last = interp.eval(expr);
        }
        return last;
    }
}
