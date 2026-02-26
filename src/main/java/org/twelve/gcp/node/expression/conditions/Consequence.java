package org.twelve.gcp.node.expression.conditions;

import org.twelve.gcp.ast.AST;
import org.twelve.gcp.inference.Inferencer;
import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.builtin.UNIT;

public class Consequence extends Block {

    public Consequence (AST ast) {
        super(ast);
    }

    @Override
    public Outline acceptInfer(Inferencer inferencer) {
        Outline o = inferencer.visit((Block) this);
        if(o instanceof UNIT){
            //this.nodes().
        }
        return o;
    }
}
