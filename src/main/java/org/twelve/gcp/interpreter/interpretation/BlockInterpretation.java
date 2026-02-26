package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.node.statement.Statement;

public class BlockInterpretation implements Interpretation<Block> {
    @Override
    public Value interpret(Block node, Interpreter interp) {
        Environment childEnv = interp.env().child();
        Environment saved = interp.env();
        interp.setEnv(childEnv);
        try {
            return evalStatements(node, interp);
        } finally {
            interp.setEnv(saved);
        }
    }

    /** Evaluate body statements; return last expression value. */
    public static Value evalStatements(org.twelve.gcp.node.expression.body.Body body, Interpreter interp) {
        Value last = UnitValue.INSTANCE;
        for (Node child : body.nodes()) {
            if (child instanceof Statement stmt) {
                last = interp.eval(stmt);
            } else if (child instanceof Expression expr) {
                last = interp.eval(expr);
            }
        }
        return last;
    }
}
