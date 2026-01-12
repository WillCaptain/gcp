package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;

public class TernaryExpression extends Selections<IfArm> {

    public TernaryExpression(IfArm arm, IfArm... arms) {
        super(arm, arms);
    }

    public TernaryExpression(AST ast) {
        super(ast);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        Arm first = this.arms().getFirst();
        Arm last = this.arms().getLast();
        sb.append(first.test().lexeme())
                .append("?")
                .append(first.consequence().lexeme(), 3, first.consequence().lexeme().length() - 2)
                .append(":")
                .append(last.consequence().lexeme(), 3, last.consequence().lexeme().length() - 2);
        return sb.toString();
    }
}