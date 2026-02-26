package org.twelve.gcp.interpreter.interpretation;

import org.twelve.gcp.interpreter.Interpretation;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;
import org.twelve.gcp.node.statement.MemberNode;

public class MemberNodeInterpretation implements Interpretation<MemberNode> {
    @Override
    public Value interpret(MemberNode node, Interpreter interpreter) {
        return interpreter.eval(node.expression());
    }
}
