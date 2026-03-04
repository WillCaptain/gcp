package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.PromiseValue;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.AwaitNode;

/**
 * Interpretation for {@link AwaitNode}.
 *
 * <p>Evaluates the inner expression to obtain a {@link PromiseValue}, then blocks
 * the calling thread until the async computation resolves. If the expression does
 * not evaluate to a {@code PromiseValue}, {@link UnitValue#INSTANCE} is returned.
 */
public class AwaitInterpretation implements Interpretation<AwaitNode> {

    @Override
    public Value interpret(AwaitNode node, Interpreter interp) {
        Value v = interp.eval(node.promise());
        if (v instanceof PromiseValue) {
            return ((PromiseValue) v).get();
        }
        return UnitValue.INSTANCE;
    }
}
