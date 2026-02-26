package org.twelve.gcp.node.expression.typeable;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.outline.Outline;

public class ThisTypeNode extends TypeNode {
    public ThisTypeNode(AST ast) {
        super(ast);
    }

    @Override
    public String lexeme() {
        return "~this";
    }
    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        return inferencer.visit(this);
    }

}
