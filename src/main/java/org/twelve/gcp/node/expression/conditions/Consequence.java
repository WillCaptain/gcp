package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferences;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.outline.Outline;

public class Consequence extends Block {

    public Consequence (AST ast) {
        super(ast);
    }

    @Override
    protected Outline accept(Inferences inferences) {
        return inferences.visit((Block) this);
    }
}
