package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Environment;
import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.UnitValue;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.expression.conditions.MatchArm;
import org.twelve.gcp.node.expression.conditions.MatchExpression;
import org.twelve.gcp.node.expression.conditions.MatchTest;

public class MatchInterpretation implements Interpretation<MatchExpression> {
    @Override
    public Value interpret(MatchExpression node, Interpreter interp) {
        Value subject = interp.eval(node.subject());

        for (MatchArm arm : node.arms()) {
            MatchTest test = arm.test();

            if (test.isElse()) return SelectionsInterpretation.evalConsequence(arm.consequence(), interp);

            Environment patternEnv = interp.env().child();
            if (!PatternMatcher.match(test.pattern(), subject, patternEnv, interp)) continue;

            if (test.condition() != null) {
                Environment saved = interp.env();
                interp.setEnv(patternEnv);
                Value guard;
                try { guard = interp.eval(test.condition()); } finally { interp.setEnv(saved); }
                if (!guard.isTruthy()) continue;
            }

            Environment saved = interp.env();
            interp.setEnv(patternEnv);
            try {
                return SelectionsInterpretation.evalConsequence(arm.consequence(), interp);
            } finally {
                interp.setEnv(saved);
            }
        }
        return UnitValue.INSTANCE;
    }
}
