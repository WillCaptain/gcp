package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;

public class IfExpression extends Selections<IfArm>{
    public IfExpression(IfArm arm, IfArm... arms) {
        super(arm, arms);
    }

    public IfExpression(AST ast) {
        super(ast);
    }

    @Override
    public String lexeme() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<this.arms().size();i++) {
            Arm arm = this.arms().get(i);
            if (arm.isElse()) {
                sb.append(" else ").append(arm.consequence().lexeme());
            } else {
                if(i>0) sb.append(" else ");
                sb.append("if(")
                        .append(arm.test().lexeme())
                        .append(")")
                        .append(arm.consequence().lexeme());
            }
        }
        return sb.toString();
    }
}
