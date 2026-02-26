package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.function.FunctionCallNode;

import java.util.List;

public class FunctionCallInterpretation implements Interpretation<FunctionCallNode> {
    @Override
    public Value interpret(FunctionCallNode node, Interpreter interp) {
        Value fn = interp.eval(node.function());
        List<Expression> args = node.arguments();
        if (args.isEmpty()) {
            return interp.apply(fn, UnitValue.INSTANCE);
        }
        Value result = fn;
        for (Expression argExpr : args) {
            result = interp.apply(result, interp.eval(argExpr));
        }
        return result;
    }
}
