package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.identifier.Identifier;
import org.twelve.gcp.node.unpack.UnpackNode;
import org.twelve.gcp.outline.Outline;

public class UnpackNodeInference implements Inference<UnpackNode>{
    @Override
    public Outline infer(UnpackNode node, Inferencer inferencer) {
        for (Identifier id : node.identifiers()) {
            node.ast().symbolEnv().defineSymbol(id.name(), node.ast().unknown(id), false, id);
            id.infer(inferencer);
        }

       return node.outline();
    }
}
