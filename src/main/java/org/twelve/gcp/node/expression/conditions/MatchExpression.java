package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Expression;

public class MatchExpression extends Selections<MatchArm>{
    private final Expression subject;
    public MatchExpression(AST ast, Expression subject) {
        super(ast);
        this.subject = this.addNode(subject);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder("match "+this.subject.lexeme()+"{");
        for (MatchArm arm : this.arms()) {
            sb.append("  "+arm.lexeme()+",\n");
        }
        return sb.append("}").toString();
    }

    @Override
    public void addArm(MatchArm arm) {
        arm.setSubject(subject);
        super.addArm(arm);
    }

    public Expression subject(){
        return this.subject;
    }
}
