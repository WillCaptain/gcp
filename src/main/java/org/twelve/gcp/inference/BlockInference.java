package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.outline.Outline;

public class BlockInference extends BodyInference<Block> {
    @Override
    public Outline infer(Block node, Inferencer inferencer) {
        Outline outline = super.infer(node, inferencer);
//        if(outline instanceof Option) {
//            ((Option) outline).options().removeIf(o -> o == ProductADT.Ignore);//remove the no return outline
//        }
        return outline;
    }
}
