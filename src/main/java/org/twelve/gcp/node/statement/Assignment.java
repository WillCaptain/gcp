package org.twelve.gcp.node.statement;

import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.Assignable;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.node.expression.body.Body;
import org.twelve.gcp.outline.Outline;

public class Assignment extends Statement {
    private final Assignable lhs;  // The variable being assigned to
    private final Expression rhs; // The expression providing the value

    public Assignment(Assignable lhs, Expression rhs) {//possible a:Type = expression
        super(lhs.ast());//Unknown means need to type infer
        this.lhs = this.addNode(lhs);
        this.rhs = this.addNode(rhs);
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
            sb.append(" = ");
            sb.append(this.rhs.lexeme());
        }
        if (this.parent instanceof Body) {
            sb.append(";");
        }
        return sb.toString();
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
    public void setInferred() {
        this.outline = this.ast().Ignore;
    }
}
