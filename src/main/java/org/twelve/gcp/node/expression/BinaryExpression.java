package org.twelve.gcp.node.expression;

import org.twelve.gcp.ast.AbstractNode;
import org.twelve.gcp.inference.operator.BinaryOperator;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.outline.Outline;

public class BinaryExpression extends Expression{
    private final Expression left;
    private final Expression right;
    private final OperatorNode<BinaryOperator> operatorNode;
    public BinaryExpression(Expression left, Expression right, OperatorNode<BinaryOperator> operator) {
        super(left.ast(), null);
        this.left = this.addNode(left);
        this.operatorNode = this.addNode(operator);
        this.right = this.addNode(right);
    }

    public Expression left(){
        return left;
    }

    public Expression right(){
        return right;
    }

    public BinaryOperator operator(){
        return operatorNode.operator();
    }

    public AbstractNode operatorNode() {
        return operatorNode;
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        return this.left.lexeme().split(":")[0]+this.operatorNode.lexeme()+this.right.lexeme().split(":")[0];
    }

}
