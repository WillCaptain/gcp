package org.twelve.gcp.node.operator;

import org.twelve.gcp.ast.*;
import org.twelve.gcp.inference.operator.Operator;

public class OperatorNode<O extends Operator> extends AbstractNode {
    private final O operator;

    public OperatorNode(AST ast, O operator) {
        super(ast,null,ast.Ignore);
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
