package org.twelve.gcp.node.expression.todo;

import org.twelve.gcp.ast.OAST;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.function.Argument;
import org.twelve.gcp.node.function.FunctionNode;

public class Lambda extends FunctionNode {
    public Lambda(Argument argument, FunctionBody body) {
        super(argument, body);
    }
}
