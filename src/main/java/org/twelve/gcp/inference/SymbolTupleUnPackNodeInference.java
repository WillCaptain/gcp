package org.twelve.gcp.inference;

import org.twelve.gcp.node.unpack.SymbolTupleUnpackNode;
import org.twelve.gcp.node.unpack.TupleUnpackNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.SymbolTuple;
import org.twelve.gcp.outline.adt.Tuple;
import org.twelve.gcp.outline.primitive.SYMBOL;

import static org.twelve.gcp.common.Tool.cast;

public class SymbolTupleUnPackNodeInference implements Inference<SymbolTupleUnpackNode> {
    @Override
    public Outline infer(SymbolTupleUnpackNode node, Inferences inferences) {
        SYMBOL symbol = cast(node.symbol().infer(inferences));
        Tuple tuple = cast(node.outline());//cast(inferences.visit((TupleUnpackNode) node));
        return new SymbolTuple(symbol,tuple);
    }
}
