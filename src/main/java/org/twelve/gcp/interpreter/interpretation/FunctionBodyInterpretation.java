package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.body.FunctionBody;

public class FunctionBodyInterpretation implements Interpretation<FunctionBody> {
    @Override
    public Value interpret(FunctionBody node, Interpreter interp) {
        return BlockInterpretation.evalStatements(node, interp);
    }
}
