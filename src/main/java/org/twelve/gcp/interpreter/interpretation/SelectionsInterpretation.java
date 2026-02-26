package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.conditions.Arm;
import org.twelve.gcp.node.expression.conditions.Consequence;
import org.twelve.gcp.node.expression.conditions.Selections;
import org.twelve.gcp.node.statement.Statement;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.interpreter.ReturnException;

public class SelectionsInterpretation implements Interpretation<Selections<?>> {
    @Override
    public Value interpret(Selections<?> node, Interpreter interp) {
        for (Arm<?> arm : node.arms()) {
            if (arm.isElse()) return evalConsequence(arm.consequence(), interp);
            Value cond = interp.eval(arm.test());
            if (cond.isTruthy()) return evalConsequence(arm.consequence(), interp);
        }
        return UnitValue.INSTANCE;
    }

    public static Value evalConsequence(Consequence consequence, Interpreter interp) {
        Environment childEnv = interp.env().child();
        Environment saved = interp.env();
        interp.setEnv(childEnv);
        try {
            Value result = UnitValue.INSTANCE;
            for (Node n : consequence.nodes()) {
                if (n instanceof Statement stmt) result = interp.eval(stmt);
            }
            return result;
        } catch (ReturnException re) {
            throw re;
        } finally {
            interp.setEnv(saved);
        }
    }
}
