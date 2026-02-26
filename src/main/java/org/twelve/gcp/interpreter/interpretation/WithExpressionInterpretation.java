package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.EntityValue;
import org.twelve.gcp.interpreter.value.FunctionValue;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.body.WithExpression;

public class WithExpressionInterpretation implements Interpretation<WithExpression> {
    @Override
    public Value interpret(WithExpression node, Interpreter interp) {
        Value resource = interp.eval(node.resource());
        Environment withEnv = interp.env().child();
        if (node.as() != null) withEnv.define(node.as().name(), resource);

        callMethodIfPresent(resource, "open", UnitValue.INSTANCE, interp);

        Environment saved = interp.env();
        interp.setEnv(withEnv);
        Value result;
        try {
            result = interp.eval(node.body());
        } finally {
            interp.setEnv(saved);
            callMethodIfPresent(resource, "close", UnitValue.INSTANCE, interp);
        }
        return result;
    }

    private void callMethodIfPresent(Value resource, String method, Value arg, Interpreter interp) {
        if (resource instanceof EntityValue ev) {
            Value m = ev.get(method);
            if (m instanceof FunctionValue) interp.apply(m, arg);
        }
    }
}
