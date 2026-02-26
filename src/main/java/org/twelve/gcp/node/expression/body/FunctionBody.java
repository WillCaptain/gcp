package org.twelve.gcp.node.expression.body;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.interpreter.Interpreter;
import org.twelve.gcp.interpreter.value.Value;

public class FunctionBody extends Body {
    public FunctionBody(AST ast) {
        super(ast);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }
    @Override
    public Value acceptInterpret(Interpreter interpreter) {
        return interpreter.visit(this);
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
