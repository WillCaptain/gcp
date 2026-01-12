package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.node.expression.Expression;

public class MatchArm extends Arm<MatchTest> {

    public MatchArm(AST ast, MatchTest test, Consequence consequence) {
        super(ast, test, consequence);
    }

    public void setSubject(Expression subject) {
        this.test.setSubject(subject);
    }

    @Override
    public MatchTest test() {
        return this.test;
    }

    @Override
    public Boolean isElse() {
        return this.test.isElse();
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        sb.append(test.lexeme());
        sb.append("->");
        if(consequence.nodes().size()==1){
            sb.append(consequence.get(0).lexeme());
        }else{
            sb.append(consequence.lexeme());
        }
        return sb.toString();
    }
}
