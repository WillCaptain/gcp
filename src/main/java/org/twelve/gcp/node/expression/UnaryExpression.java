package org.twelve.gcp.node.expression;

import org.twelve.gcp.exception.GCPErrorReporter;
import org.twelve.gcp.exception.GCPErrCode;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.inference.operator.UnaryOperator;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.outline.Outline;

public class UnaryExpression extends Expression {
    private Expression operand;
    private OperatorNode<UnaryOperator> operatorNode;

    private UnaryPosition position;
    public UnaryExpression(Expression operand, OperatorNode<UnaryOperator> operator, UnaryPosition position) {
        super(operand.ast(), null);
        if(!operator.operator().contains(position)){
            GCPErrorReporter.report(operator, GCPErrCode.UNARY_POSITION_MISMATCH);
        }
        this.operatorNode = this.addNode(operator);
        this.operand = this.addNode(operand);
        this.position = position;
    }

    public Expression operand(){
        return this.operand;
    }
    @Override
    public Outline outline() {
        return this.operand.outline();
    }

    public UnaryPosition position(){
        return this.position;
    }

    public UnaryOperator operator(){
        return this.operatorNode.operator();
    }

    public OperatorNode<UnaryOperator> operatorNode(){
        return this.operatorNode;
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        if(position==UnaryPosition.POSTFIX){
            return operatorNode.toString()+operand.toString();
        }else{
            return operand.toString()+operatorNode.toString();
        }
    }
}
