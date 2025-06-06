package org.twelve.gcp.node.expression.body;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class FunctionBody extends Body {
    public FunctionBody(AST ast) {
        super(ast);
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }

    @Override
    public String lexeme() {
        String arrow = "->";
        if(this.nodes().size()==1){
            return arrow+this.nodes().getFirst().lexeme();
        }else{
            return arrow+super.lexeme();
        }
    }
}
