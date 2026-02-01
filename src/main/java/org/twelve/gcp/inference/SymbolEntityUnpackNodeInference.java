package org.twelve.gcp.inference;

import org.twelve.gcp.node.unpack.EntityUnpackNode;
import org.twelve.gcp.node.unpack.SymbolEntityUnpackNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.SymbolEntity;
import org.twelve.gcp.outline.primitive.SYMBOL;

import static org.twelve.gcp.common.Tool.cast;

public class SymbolEntityUnpackNodeInference implements Inference<SymbolEntityUnpackNode> {
    @Override
    public Outline infer(SymbolEntityUnpackNode node, Inferences inferences) {
        SYMBOL symbol = cast(node.symbol().infer(inferences));
        Entity entity = cast(node.outline());//cast(inferences.visit((EntityUnpackNode) node));
        return new SymbolEntity(symbol,entity);
    }
}
