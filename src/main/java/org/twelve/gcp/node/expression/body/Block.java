package org.twelve.gcp.node.expression.body;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.outline.Outline;

public class Block extends Body {
    public Block(AST ast) {
        super(ast);
    }

    @Override
    public Outline accept(Inferences inferences) {
        return inferences.visit(this);
    }
}
