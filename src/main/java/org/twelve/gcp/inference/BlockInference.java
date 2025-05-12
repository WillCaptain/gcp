package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.body.Block;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Option;
import org.twelve.gcp.outline.adt.ProductADT;

public class BlockInference extends BodyInference<Block> {
    @Override
    public Outline infer(Block node, Inferences inferences) {
        Outline outline = super.infer(node, inferences);
//        if(outline instanceof Option) {
//            ((Option) outline).options().removeIf(o -> o == ProductADT.Ignore);//remove the no return outline
//        }
        return outline;
    }
}
