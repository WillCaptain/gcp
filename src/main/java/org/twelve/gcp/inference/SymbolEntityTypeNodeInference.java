package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.SymbolEntityTypeTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Entity;
import org.twelve.gcp.outline.adt.SymbolEntity;
import org.twelve.gcp.outline.primitive.SYMBOL;

import static org.twelve.gcp.common.Tool.cast;

public class SymbolEntityTypeNodeInference implements Inference<SymbolEntityTypeTypeNode> {
    @Override
    public Outline infer(SymbolEntityTypeTypeNode node, Inferences inferences) {
        Entity entity = cast(new EntityTypeNodeInference().infer(node,inferences));
        SYMBOL symbol = cast(node.symbol().infer(inferences));
        return new SymbolEntity(symbol,entity);
    }
}
