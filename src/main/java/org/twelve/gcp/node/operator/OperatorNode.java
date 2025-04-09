package org.twelve.gcp.node.operator;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.inference.operator.Operator;
import org.twelve.gcp.outline.Outline;

public class OperatorNode<O extends Operator> extends ONode {
    private final O operator;

    public OperatorNode(OAST ast, O operator) {
        super(ast,null,Outline.Ignore);
        this.operator = operator;
    }

    public O operator(){
        return this.operator;
    }

    @Override
    public String lexeme() {
        return this.operator().symbol();
    }
}
