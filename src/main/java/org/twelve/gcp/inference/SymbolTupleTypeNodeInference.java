package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.SymbolTupleTypeTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Tuple;
import org.twelve.gcp.outline.adt.SymbolTuple;
import org.twelve.gcp.outline.primitive.SYMBOL;

import static org.twelve.gcp.common.Tool.cast;

public class SymbolTupleTypeNodeInference implements Inference<SymbolTupleTypeTypeNode> {
    @Override
    public Outline infer(SymbolTupleTypeTypeNode node, Inferences inferences) {
        Tuple tuple = cast(new TupleTypeNodeInference().infer(node, inferences));
        SYMBOL symbol = cast(node.symbol().infer(inferences));
        return new SymbolTuple(symbol, tuple);
    }
}
