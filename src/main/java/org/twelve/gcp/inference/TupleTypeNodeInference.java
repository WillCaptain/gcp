package org.twelve.gcp.inference;

import org.twelve.gcp.node.expression.typeable.TupleTypeNode;
import org.twelve.gcp.outline.Outline;
import org.twelve.gcp.outline.adt.Tuple;

import static org.twelve.gcp.common.Tool.cast;

public class TupleTypeNodeInference implements Inference<TupleTypeNode>{
    @Override
    public Outline infer(TupleTypeNode node, Inferences inferences) {
        return new Tuple(cast(new EntityTypeNodeInference().infer(node,inferences)));
    }
}
