package org.twelve.gcp.node.expression;

import org.twelve.gcp.inference.operator.AssignableOperator;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.operator.OperatorNode;
import org.twelve.gcp.outline.Outline;

public class Assignment extends Expression {
    private final Assignable lhs;  // The variable being assigned to
    private final Expression rhs; // The expression providing the value
    private final OperatorNode<AssignableOperator> operator;

    public Assignment(Assignable lhs, Expression rhs) {//possible a:Type = expression
        this(lhs,rhs,new OperatorNode<>(lhs.ast(),AssignableOperator.EQUALS));//Unknown means need to type infer
    }

    public Assignment(Assignable lhs, Expression rhs, OperatorNode<AssignableOperator> operator) {//possible a:Type = expression
        super(lhs.ast(),null);//Unknown means need to type infer
        this.lhs = this.addNode(lhs);
        this.rhs = this.addNode(rhs);
        this.operator = operator;
    }

    @Override
    public Outline outline() {
        return this.ast().Ignore;
    }

    public Assignable lhs() {
        return this.lhs;
    }

    public Expression rhs() {
        return this.rhs;
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
            sb.append(this.lhs.lexeme());
        if (this.rhs != null){
            sb.append(" "+this.operator.lexeme()+" ");
            sb.append(this.rhs.lexeme());
        }
//        if (this.parent instanceof Body) {
//            sb.append(";");
//        }
        return sb.toString();
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
    public void setInferred() {
        this.outline = this.ast().Ignore;
    }
}
