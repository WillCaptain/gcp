package org.twelve.gcp.node.expression.conditions;

import lombok.Setter;
import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.Expression;
import org.twelve.gcp.outline.Outline;

public class MatchTest extends Expression {
    @Setter
    private Expression subject;
    private final Expression pattern;
    private final Expression condition;

    public MatchTest(AST ast, Expression pattern, Expression condition) {
        super(ast, null);
        this.pattern = this.addNode(pattern);
        this.condition = this.addNode(condition);
    }

    public Expression subject(){
        return this.subject;
    }

    public Expression pattern(){
        return this.pattern;
    }

    public Expression condition(){
        return this.condition;
    }
    @Override
    public Outline accept(Inferences inferences) {

        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder(pattern.lexeme());
        if(condition!=null){
            sb.append(" if ").append(condition.lexeme());
        }
        return sb.toString();
    }

    public Boolean isElse() {
        return this.pattern.lexeme().equals("_") && condition==null;
    }
}
